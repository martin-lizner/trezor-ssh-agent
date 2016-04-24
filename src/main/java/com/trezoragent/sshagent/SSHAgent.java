package com.trezoragent.sshagent;

import com.trezoragent.struct.PuttyStruct64;
import com.trezoragent.struct.PuttyStruct32;
import com.trezoragent.struct.PublicKeyDTO;
import com.trezoragent.struct.PuttyStruct;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.*;
import com.sun.jna.platform.win32.WinUser.WindowProc;
import com.trezoragent.exception.ActionCancelledException;
import com.trezoragent.exception.DeviceTimeoutException;
import com.trezoragent.exception.GetIdentitiesFailedException;
import com.trezoragent.exception.SignFailedException;
import com.trezoragent.gui.TrayProcess;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Level;
import static com.trezoragent.utils.AgentConstants.*;
import com.trezoragent.utils.AgentUtils;
import com.trezoragent.utils.LocalizedLogger;
import java.util.List;
import java.util.logging.Logger;
import org.spongycastle.util.encoders.Base64;

/**
 *
 * @author Martin Lizner
 *
 */
public class SSHAgent implements WindowProc {

    private User32 libU = null;
    private Kernel32 libK = null;
    private HWND hWnd = null;
    private WinNT.HANDLE sharedFile;
    private Pointer sharedMemory;

    private boolean createdCorrectly = false;
    private boolean mainLoopStarted = false;

    private HANDLE mutex = null; // mutex ref, for installer

    public SSHAgent() throws Exception {
        initCoreClasses();
        if (checkIfNoPageantProcessIsRunning()) {
            createProcess();
        } else {
            TrayProcess.createErrorWindow(LocalizedLogger.getLocalizedMessage("PAGEANT_IS_RUNNING"));
        }
    }

    /*
     * Creates and registers Windows process
     */
    private HWND createWindowsProcess() {

        WString windowClass = new WString(APPNAME);
        HMODULE hInst = libK.GetModuleHandle("");
        WNDCLASSEX wndclass = new WNDCLASSEX();
        wndclass.hInstance = hInst;
        wndclass.lpfnWndProc = SSHAgent.this;
        wndclass.lpszClassName = windowClass;
        // register window class
        libU.RegisterClassEx(wndclass);
        getLastError();
        // create new window
        HWND winprocess = libU
                .CreateWindowEx(
                        User32.WS_OVERLAPPED,
                        windowClass,
                        APPNAME,
                        0, 0, 0, 0, 0,
                        null, // WM_DEVICECHANGE contradicts parent=WinUser.HWND_MESSAGE
                        null, hInst, null);

        mutex = libK.CreateMutex(null, false, MUTEX_NAME);
        return winprocess;
    }

    /*
     * Start listening to Windows messages 
     */
    public void startMainLoop() {
        mainLoopStarted = true;
        MSG msg = new MSG();
        while (libU.GetMessage(msg, null, 0, 0) == 1) {
            libU.TranslateMessage(msg);
            libU.DispatchMessage(msg);
        }
    }

    /*
     * This method is called by Windows to deliver message
     */
    @Override
    public LRESULT callback(HWND hwnd, int uMsg, WPARAM wParam, LPARAM lParam) {
        Logger.getLogger(SSHAgent.class.getName()).log(Level.FINE, "Windows requests operation: {0}", uMsg);

        switch (uMsg) {
            case MY_WM_COPYDATA: {
                return processMessage(hwnd, wParam, lParam);
            }
            case WinUser.WM_CREATE: {
                return new LRESULT(0);
            }
            case WinUser.WM_DESTROY: {
                libU.PostQuitMessage(0);
                return new LRESULT(0);
            }
            default:
                return libU.DefWindowProc(hwnd, uMsg, wParam, lParam);
        }
    }

    private int getLastError() {
        int rc = libK.GetLastError();
        if (rc != 0) {
            Logger.getLogger(SSHAgent.class.getName()).log(Level.SEVERE, "error {0}", rc);
        }
        return rc;
    }

    private LRESULT processMessage(HWND hwnd, WPARAM wParam, LPARAM lParam) {
        WinBase.SECURITY_ATTRIBUTES psa = null;
        String mapname = readFileNameFromInput(lParam);
        sharedFile
                = libK.CreateFileMapping(WinBase.INVALID_HANDLE_VALUE,
                        psa,
                        WinNT.PAGE_READWRITE,
                        0,
                        8192, // AGENT_MAX_MSGLEN
                        mapname);

        sharedMemory
                = Kernel32.INSTANCE.MapViewOfFile(sharedFile,
                        WinNT.SECTION_MAP_WRITE,
                        0, 0, 0);

        int ret = answerIfDevicePresent(sharedMemory);
        disconnectFromSharedMemory();

        return new LRESULT(ret);
    }

    /*
     *  Method answers to recieved message and writes response to shared memory
     *  1 = success, 0 = fail - Putty protocol defined
     */
    private int answerMessage(Pointer sharedMemory) {
        byte[] buff = new byte[5];
        sharedMemory.read(0, buff, 0, 5);

        byte type = buff[4];
        switch (type) {
            case SSH2_AGENTC_REQUEST_IDENTITIES:
                processKeysRequest(sharedMemory);
                return 1;
            case SSH2_AGENTC_SIGN_REQUEST:
                processSignRequest(sharedMemory);
                return 1;
            default:
                writeAndLogFailure(sharedMemory, "Request for unsupported operation: " + type);
                return 0;

        }
    }

    private void processKeysRequest(final Pointer sharedMemory) {
        java.util.List<PublicKeyDTO> certs;
        try {
            certs = TrezorWrapper.getIdentitiesResponse(true);
            ByteBuffer ret = writeCertificatesToFrame(certs, SSH2_AGENT_IDENTITIES_ANSWER);
            sharedMemory.write(0, ret.array(), 0, ret.array().length);

        } catch (DeviceTimeoutException ex) {
            TrayProcess.handleException(ex);
        } catch (GetIdentitiesFailedException ex) {
            Logger.getLogger(SSHAgent.class.getName()).log(Level.SEVERE, "Operation {0} failed", "SSH2_AGENT_GET_IDENTITIES");
        }
    }

    private byte[] getDataFromRequest(Pointer sharedMemory, int offset) {
        byte[] length = new byte[4];
        sharedMemory.read(offset, length, 0, 4);
        ByteBuffer bb = ByteBuffer.wrap(length);
        int dataLength = bb.getInt();
        byte[] data = new byte[dataLength];
        sharedMemory.read(offset + 4, data, 0, dataLength);
        return data;

    }

    private String readFileNameFromInput(LPARAM lParam) {
        Class structPlatformClass = (Platform.is64Bit()) ? PuttyStruct64.class : PuttyStruct32.class;
        PuttyStruct ps = (PuttyStruct) PuttyStruct.newInstance(structPlatformClass, new Pointer(lParam.longValue()));
        ps.read();
        return new String(ps.lpData.getString(0).getBytes(Charset.forName("US-ASCII")));
    }

    public void disconnectFromSharedMemory() {
        if (sharedMemory != null) {
            libK.UnmapViewOfFile(sharedMemory);
        }
        if (sharedFile != null) {
            libK.CloseHandle(sharedFile);
        }
    }

    private int answerIfDevicePresent(Pointer sharedMemory) {
        if (AgentUtils.checkDeviceAvailable()) {
            return answerMessage(sharedMemory);
        } else {
            writeAndLogFailure(sharedMemory, "Device not available.");
            return 0;
        }
    }

    private void writeAndLogFailure(Pointer sharedMemory, String messageToLog) {
        Logger.getLogger(SSHAgent.class.getName()).log(Level.SEVERE, messageToLog);
        byte[] buff = new byte[5];
        buff[4] = SSH_AGENT_FAILURE;
        buff[1] = 1;
        sharedMemory.write(0, buff, 0, buff.length);
    }

    private void processSignRequest(Pointer sharedMemory) {
        byte[] keyInBytes = getDataFromRequest(sharedMemory, 5); // TODO: validate pubkey again just to be sure sign operation works ok
        byte[] challengeData = getDataFromRequest(sharedMemory, 5 + 4 + keyInBytes.length);
        byte[] signedDataRaw;
        byte[] signedData;
        byte[] userName = unframeUsernameFromChallengeBytes(challengeData);

        Logger.getLogger(SSHAgent.class.getName()).log(Level.FINE, "Server sent challenge: ", Base64.toBase64String(challengeData));
        Logger.getLogger(SSHAgent.class.getName()).log(Level.FINE, "Effective username: ", new String(userName));

        try {
            signedDataRaw = TrezorWrapper.signChallenge(challengeData, userName);
            if (signedDataRaw == null || signedDataRaw.length != 65) {
                throw new SignFailedException("HW sign response must have 65 bytes, length: " + signedDataRaw.length);
            }

            signedData = AgentUtils.createSSHSignResponse(signedDataRaw);

            if (signedData != null) {
                sharedMemory.write(0, signedData, 0, signedData.length);
                TrayProcess.createInfo(LocalizedLogger.getLocalizedMessage("CERT_USE_SUCCESS", new String(userName), TrayProcess.trezorService.getDeviceLabel()));
            } else {
                TrayProcess.createWarning(LocalizedLogger.getLocalizedMessage("CERT_USED_ERROR"));
            }
        } catch (DeviceTimeoutException | SignFailedException ex) {
            TrayProcess.handleException(ex);
        } catch (ActionCancelledException ex) {
            Logger.getLogger(SSHAgent.class.getName()).log(Level.FINE, "Sign operation cancelled on HW.");
        }
    }

    private int getResponseLength(List<PublicKeyDTO> certs) {
        int length = 4 + 1 + 4; // total length (1x int) + result code (1x byte) + no. of keys (1x int)
        for (PublicKeyDTO i : certs) {
            length += 4;
            length += i.getbPublicKey().length;
            length += 4;
            length += i.getbComment().length;
        }
        return length;
    }

    private ByteBuffer writeCertificatesToFrame(List<PublicKeyDTO> certs, byte resultCode) {
        int responseLength = getResponseLength(certs);
        ByteBuffer ret = ByteBuffer.allocate(responseLength);
        ret.put(AgentUtils.frameArray(writeCertificatesToArray(certs, resultCode, responseLength - 4)));
        return ret;
    }

    private byte[] writeCertificatesToArray(List<PublicKeyDTO> certs, byte resultCode, int frameLength) {
        ByteBuffer ret = ByteBuffer.allocate(frameLength);

        ret.put(resultCode);
        if (!certs.isEmpty()) {
            ret.putInt(certs.size()); // number of keys (not byte size)
            for (PublicKeyDTO i : certs) {
                ret.put(AgentUtils.frameArray(i.getbPublicKey()));
                ret.put(AgentUtils.frameArray(i.getbComment()));
            }
        }
        return ret.array();
    }

    private void initCoreClasses() throws Exception {
        try {
            libU = User32.INSTANCE;
            libK = Kernel32.INSTANCE;
        } catch (java.lang.UnsatisfiedLinkError | java.lang.NoClassDefFoundError ex) {
            TrayProcess.handleException(ex);
            throw new Exception(ex.toString());
        }
    }

    private boolean checkIfNoPageantProcessIsRunning() {
        HWND isRunning = libU.FindWindow(APPNAME, APPNAME);
        return isRunning == null;
    }

    private void createProcess() {
        hWnd = createWindowsProcess();
        setCreatedCorrectly(true);
        Logger.getLogger(SSHAgent.class.getName()).log(Level.INFO, "Process started successfully");
    }

    public boolean isCreatedCorrectly() {
        return createdCorrectly;
    }

    private synchronized void setCreatedCorrectly(boolean value) {
        createdCorrectly = value;
    }

    public boolean isMainLoopStarted() {
        return mainLoopStarted;
    }

    public void exitProcess() {
        Logger.getLogger(SSHAgent.class.getName()).log(Level.FINE, "Sending exit signal.");
        libU.DestroyWindow(hWnd);
        libK.CloseHandle(mutex); // just in case, mutex should be destroyed by now by process exit
        setCreatedCorrectly(false);

        //trezorService.getWallet().softDetach();
        TrayProcess.trezorService.getWallet().disconnect();
        //TrayProcess.trezorService.getClient().disconnect();
    }

    private byte[] unframeUsernameFromChallengeBytes(byte[] challengeData) {
        byte[] username;

        ByteBuffer bb = ByteBuffer.wrap(challengeData, 0, challengeData.length);
        int dataLength = bb.getInt(0); // = should be 32 bytes, random data generated on SSH server side      
        bb.position(4 + dataLength + 4 + 1); // forward buffer to possition where username frame starts
        int userNameLength = bb.getInt(4 + dataLength + 1); // determine the length of username
        username = new byte[userNameLength];
        bb.get(username, 0, userNameLength);

        return username;
    }
}
