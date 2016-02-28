package com.trezoragent.sshagent;

import com.google.common.base.Charsets;
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
import com.trezoragent.exception.KeyStoreLoadException;
import com.trezoragent.gui.TrayProcess;
import com.trezoragent.utils.AgentConstants;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Level;
import static com.trezoragent.utils.AgentConstants.*;
import com.trezoragent.utils.LocalizedLogger;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.logging.Logger;
import static org.multibit.hd.hardware.core.utils.IdentityUtils.KEY_PREFIX;
import org.spongycastle.pqc.math.linearalgebra.ByteUtils;

/**
 * 
 * @author Martin Lizner
 * 
 */

public class SSHAgent implements WindowProc {

    private final int MY_WM_COPYDATA = 0x004A;
    private final String APPNAME = "Pageant";
    
    /*
     * SSH-1 and OpenSSH SSH-2 protocol commons
     */
    private final int SSH_AGENT_FAILURE = 5;
    
    /*
     * OpenSSH protocol SSH-2
     */
    private final byte SSH2_AGENTC_REQUEST_IDENTITIES = 11;
    private final byte SSH2_AGENT_IDENTITIES_ANSWER = 12;
    private final byte SSH2_AGENTC_SIGN_REQUEST = 13;
    private final byte SSH2_AGENT_SIGN_RESPONSE = 14;

    private User32 libU = null;
    private Kernel32 libK = null;
    private HWND hWnd = null;
    
    private boolean createdCorrectly = false;
    private boolean mainLoopStarted = false;

    private HANDLE mutex = null; // mutex ref, for installer
    TrezorService trezorService;

    public SSHAgent() throws Exception {
        initCoreClasses();
        if (checkIfNoPageantProcessIsRunning()) {
            createProcess();
        } else {
            TrayProcess.createErrorWindow(LocalizedLogger.getLocalizedMessage("PAGEANT_IS_RUNNING"));
        }
    }

    public void setTrezorService(TrezorService trezorService) {
        this.trezorService = trezorService;
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
        Logger.getLogger(SSHAgent.class.getName()).log(Level.FINE, "Incoming request for operation: {0}", uMsg);

        switch (uMsg) {
            case MY_WM_COPYDATA: {
                return processMessage(hwnd, wParam, lParam);
            }
            case WinUser.WM_CREATE: {
                Logger.getLogger(SSHAgent.class.getName()).log(Level.FINE, "onCreate: WM_CREATE ");
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
        WinNT.HANDLE sharedFile;
        Pointer sharedMemory;
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
        disconnectFromSharedMemory(sharedFile, sharedMemory);

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
                Logger.getLogger(SSHAgent.class.getName()).log(Level.INFO, "Request for operation: {0}", "SSH2_AGENTC_REQUEST_IDENTITIES");
                processKeysRequest(sharedMemory);
                Logger.getLogger(SSHAgent.class.getName()).log(Level.INFO, "Operation {0} executed successfully", "SSH2_AGENTC_REQUEST_IDENTITIES");
                return 1;
            case SSH2_AGENTC_SIGN_REQUEST:
                Logger.getLogger(SSHAgent.class.getName()).log(Level.INFO, "Request for operation: {0}", "SSH2_AGENTC_SIGN_REQUEST");
                processSignRequest(sharedMemory);
                Logger.getLogger(SSHAgent.class.getName()).log(Level.INFO, "Operation {0} executed successfully", "SSH2_AGENTC_SIGN_REQUEST");
                return 1;
            default:
                writeAndLogFailure(sharedMemory, "Request for unsupported operation: " + type);
                return 0;

        }
    }

    private void processKeysRequest(Pointer sharedMemory) {       
        java.util.List<PublicKeyDTO> certs = TrezorWrapper.getIdentities(trezorService, true);
            
        ByteBuffer ret = writeCertificatesToBuffer(certs, SSH2_AGENT_IDENTITIES_ANSWER);
        sharedMemory.write(0, ret.array(), 0, ret.array().length);

    }

    private byte[] getDataFromRequest(Pointer sharedMemory, int offset) {
        byte[] length = new byte[4];
        sharedMemory.read(offset, length, 0, 4);
        ByteBuffer bb = ByteBuffer.wrap(length);
        int dataLength = bb.getInt();
        Logger.getLogger(SSHAgent.class.getName()).log(Level.FINER, "reading data length: {0}", dataLength);
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

    private void disconnectFromSharedMemory(WinNT.HANDLE sharedFile, Pointer sharedMemory) {
        if (sharedMemory != null) {
            libK.UnmapViewOfFile(sharedMemory);
        }
        if (sharedFile != null) {
            libK.CloseHandle(sharedFile);
        }
    }

    private int answerIfDevicePresent(Pointer sharedMemory) {
        if (trezorService.getHardwareWalletService().isWalletPresent()) {
            return answerMessage(sharedMemory);
        } else {
            TrayProcess.createWarning(LocalizedLogger.getLocalizedMessage("DEVICE_NOT_PRESENT"));
            writeAndLogFailure(sharedMemory, "Device not found!");
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

    public byte[] frameArray(byte[] array) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + array.length);
        buffer.putInt(array.length);
        buffer.put(array);
        return buffer.array();
    }

    public byte[] frameArray(byte[] array1, byte[] array2) {
        return frameArray(ByteUtils.concatenate(array1, array2));
    }

    private void processSignRequest(Pointer sharedMemory) {
        byte[] keyInBytes = getDataFromRequest(sharedMemory, 5);
        byte[] challengeData = getDataFromRequest(sharedMemory, 5 + 4 + keyInBytes.length);
        byte[] signedDataRaw;
        byte[] signedData;

        byte[] zero = {(byte) 0x00};
        byte[] respCode = {SSH2_AGENT_SIGN_RESPONSE};

        try {
            signedDataRaw = TrezorWrapper.signChallenge(trezorService, challengeData);

            byte[] noOctet = ByteUtils.subArray(signedDataRaw, 1, signedDataRaw.length); // remove first byte from 65byte array
            byte[] xSign = ByteUtils.subArray(noOctet, 0, 32); // devide 64byte array into halves
            byte[] ySign = ByteUtils.subArray(noOctet, 32, noOctet.length);
            xSign = ByteUtils.concatenate(zero, xSign); // add zero byte
            ySign = ByteUtils.concatenate(zero, ySign);

            byte[] sigBytes = ByteUtils.concatenate(frameArray(xSign), frameArray(ySign));
            byte[] dataArray = frameArray(frameArray(KEY_PREFIX.getBytes(Charsets.UTF_8)), frameArray(sigBytes));
            signedData = frameArray(respCode, dataArray);
            
            if (signedData == null) {
                TrayProcess.createWarning(LocalizedLogger.getLocalizedMessage("CERT_USED_ERROR"));
            } else {
                sharedMemory.write(0, signedData, 0, signedData.length);
                TrayProcess.createInfo(LocalizedLogger.getLocalizedMessage("CERT_USE_SUCCESS") + AgentConstants.KEY_COMMENT);
            }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | InvalidKeyException | URISyntaxException | KeyStoreLoadException | InterruptedException ex) {
            TrayProcess.handleException(ex);
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

    private ByteBuffer writeCertificatesToBuffer(List<PublicKeyDTO> certs, byte response) {
        int responseLength = getResponseLength(certs);
        ByteBuffer ret = ByteBuffer.allocate(responseLength);
        ret.putInt(responseLength);
        ret.put(response);
        if (!certs.isEmpty()) {
            ret.putInt(certs.size());
            for (PublicKeyDTO i : certs) {
                ret.putInt(i.getbPublicKey().length);
                ret.put(i.getbPublicKey());
                ret.putInt(i.getbComment().length);
                ret.put(i.getbComment());
            }
        }
        return ret;
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
    }
}
