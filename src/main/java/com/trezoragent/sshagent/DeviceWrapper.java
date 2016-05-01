package com.trezoragent.sshagent;

import com.trezoragent.exception.ActionCancelledException;
import com.trezoragent.exception.DeviceTimeoutException;
import com.trezoragent.exception.GetIdentitiesFailedException;
import com.trezoragent.exception.SignFailedException;
import com.trezoragent.gui.TrayProcess;
import static com.trezoragent.gui.TrayProcess.settings;
import com.trezoragent.struct.PublicKeyDTO;
import java.util.ArrayList;
import java.util.List;
import org.multibit.hd.hardware.core.domain.Identity;
import java.util.logging.Logger;
import com.trezoragent.utils.AgentUtils;
import com.trezoragent.utils.AgentConstants;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import javax.swing.Timer;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author martin.lizner
 */
public class DeviceWrapper {

    public static void getIdentitiesRequest() { // directly used only for GUI calls with explicit swing timer
        Logger.getLogger(DeviceWrapper.class.getName()).log(Level.INFO, "Request for operation: {0}", "SSH2_AGENT_GET_IDENTITIES"); // TODO: differentiate in log between call from GUI (e.g. GUI_GET_IDENTITIES) or from SSH Client (SSH2_AGENT_GET_IDENTITIES)
        if (!AgentUtils.checkDeviceAvailable()) {
            AgentUtils.stopGUITimer();
            return;
        }

        // Load settings from file
        String bip32Path = AgentUtils.readSetting(settings, AgentConstants.SETTINGS_KEY_BIP32_URI, AgentConstants.BIP32_SSHURI);
        String bip32Index = AgentUtils.readSetting(settings, AgentConstants.SETTINGS_KEY_BIP32_INDEX, AgentConstants.BIP32_INDEX.toString()); // TODO: fix types

        TrayProcess.deviceService.getHardwareWalletService().requestPublicKeyForIdentity(URI.create(bip32Path), new Integer(bip32Index), AgentConstants.CURVE_NAME, false);
    }

    public static List<PublicKeyDTO> getIdentitiesResponse(Boolean stripPrefix) throws DeviceTimeoutException, GetIdentitiesFailedException {
        String trezorKey;
        List<PublicKeyDTO> idents = new ArrayList<>();

        AgentUtils.stopGUITimer();
        getIdentitiesRequest();

        if (!AgentUtils.checkDeviceAvailable()) {
            return idents;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(TrayProcess.deviceService.checkoutAsyncKeyData());

        try {
            trezorKey = future.get(AgentConstants.KEY_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            //Logger.getLogger(TrezorWrapper.class.getName()).log(Level.SEVERE, "Timeout when waiting for device");
            TrayProcess.deviceService.getHardwareWalletService().requestCancel();
            throw new DeviceTimeoutException();
        }

        if (AgentConstants.GET_IDENTITIES_FAILED_STRING.equals(trezorKey)) {
            TrayProcess.deviceService.getHardwareWalletService().requestCancel();
            throw new GetIdentitiesFailedException();
        }

        if (stripPrefix) { // remove ecdsa-sha2... from beginning
            String[] keySplit = trezorKey.split(" ");
            if (keySplit[1] != null) {
                trezorKey = keySplit[1];
            }
        }

        PublicKeyDTO p = new PublicKeyDTO();
        p.setbPublicKey(DatatypeConverter.parseBase64Binary(trezorKey));
        p.setsPublicKey(trezorKey);
        p.setsComment(TrayProcess.deviceService.getDeviceLabel());
        p.setbComment(TrayProcess.deviceService.getDeviceLabel().getBytes());
        idents.add(p);
        //TrayProcess.trezorService.checkoutAsyncKeyData(); // null key        

        return idents;
    }

    public static byte[] signChallenge(byte[] challengeHidden, byte[] challengeVisualBytes) throws DeviceTimeoutException, SignFailedException, ActionCancelledException {
        byte[] signature;
        Logger.getLogger(DeviceWrapper.class.getName()).log(Level.INFO, "Request for operation: {0}", "SSH2_AGENT_SIGN_REQUEST");

        if (!AgentUtils.checkDeviceAvailable()) {
            return AgentConstants.SIGN_FAILED_BYTE;
        }

        String bip32Path = AgentUtils.readSetting(settings, AgentConstants.SETTINGS_KEY_BIP32_URI, AgentConstants.BIP32_SSHURI);
        String bip32Index = AgentUtils.readSetting(settings, AgentConstants.SETTINGS_KEY_BIP32_INDEX, AgentConstants.BIP32_INDEX.toString()); // TODO: fix types

        String challengeVisual = (challengeVisualBytes != null && challengeVisualBytes.length > 0)
                ? new String(challengeVisualBytes) : "Warn: No user given!"; // display username contained in SSH Server challenge, if no username is provided by SSH Server display warning

        Identity identity = new Identity(URI.create(bip32Path), new Integer(bip32Index), challengeHidden, challengeVisual, AgentConstants.CURVE_NAME);

        TrayProcess.deviceService.getHardwareWalletService().signIdentity(identity);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<byte[]> future = executor.submit(TrayProcess.deviceService.checkoutAsyncSignData());

        try {
            signature = future.get(AgentConstants.SIGN_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            TrayProcess.deviceService.getHardwareWalletService().requestCancel();
            throw new DeviceTimeoutException();
        }

        if (Arrays.equals(AgentConstants.SIGN_FAILED_BYTE, signature)) {
            TrayProcess.deviceService.getHardwareWalletService().requestCancel();
            throw new SignFailedException("Sign operation failed on HW.");
        }

        if (Arrays.equals(AgentConstants.SIGN_CANCELLED_BYTE, signature)) {
            throw new ActionCancelledException();
        }

        //TrayProcess.trezorService.checkoutAsyncSignData(); // null sign data        
        return signature;
    }

}
