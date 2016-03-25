package com.trezoragent.sshagent;

import com.trezoragent.exception.ActionCancelledException;
import com.trezoragent.exception.DeviceTimeoutException;
import com.trezoragent.exception.GetIdentitiesFailedException;
import com.trezoragent.exception.SignFailedException;
import com.trezoragent.gui.TrayProcess;
import com.trezoragent.struct.PublicKeyDTO;
import java.util.ArrayList;
import java.util.List;
import org.multibit.hd.hardware.core.domain.Identity;
import java.util.logging.Logger;
import com.trezoragent.utils.AgentUtils;
import com.trezoragent.utils.AgentConstants;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author martin.lizner
 */
public class TrezorWrapper {

    public static void getIdentitiesRequest() { // directly used only for GUI calls with explicit swing timer
        Logger.getLogger(TrezorWrapper.class.getName()).log(Level.INFO, "Request for operation: {0}", "SSH2_AGENT_GET_IDENTITIES"); // TODO: differentiate in log between call from GUI (e.g. GUI_GET_IDENTITIES) or from SSH Client (SSH2_AGENT_GET_IDENTITIES)
        if (!AgentUtils.checkDeviceAvailable()) {
            return;
        }
        TrayProcess.trezorService.getHardwareWalletService().requestPublicKeyForIdentity(AgentConstants.SSHURI, 0, AgentConstants.CURVE_NAME, false);
    }

    public static List<PublicKeyDTO> getIdentitiesResponse(Boolean stripPrefix) throws DeviceTimeoutException, GetIdentitiesFailedException {
        String trezorKey;
        List<PublicKeyDTO> idents = new ArrayList<>();

        getIdentitiesRequest();

        if (!AgentUtils.checkDeviceAvailable()) {
            return idents;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(TrayProcess.trezorService.checkoutAsyncKeyData());

        try {
            trezorKey = future.get(AgentConstants.KEY_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            //Logger.getLogger(TrezorWrapper.class.getName()).log(Level.SEVERE, "Timeout when waiting for device");
            TrayProcess.trezorService.getHardwareWalletService().requestCancel();
            throw new DeviceTimeoutException();
        }

        if (AgentConstants.GET_IDENTITIES_FAILED_STRING.equals(trezorKey)) {
            TrayProcess.trezorService.getHardwareWalletService().requestCancel();
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
        p.setsComment(AgentConstants.KEY_COMMENT);
        p.setbComment(AgentConstants.KEY_COMMENT.getBytes());
        idents.add(p);
        //TrayProcess.trezorService.checkoutAsyncKeyData(); // null key        

        return idents;
    }

    public static byte[] signChallenge(byte[] challengeHidden) throws DeviceTimeoutException, SignFailedException, ActionCancelledException {
        byte[] signature;
        Logger.getLogger(TrezorWrapper.class.getName()).log(Level.INFO, "Request for operation: {0}", "SSH2_AGENT_SIGN_REQUEST");

        if (!AgentUtils.checkDeviceAvailable()) {
            return AgentConstants.SIGN_FAILED_BYTE;
        }

        String challengeVisual = AgentUtils.getCurrentTimeStamp();
        Identity identity = new Identity(AgentConstants.SSHURI, 0, challengeHidden, challengeVisual, AgentConstants.CURVE_NAME);

        TrayProcess.trezorService.getHardwareWalletService().signIdentity(identity);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<byte[]> future = executor.submit(TrayProcess.trezorService.checkoutAsyncSignData());

        try {
            signature = future.get(AgentConstants.SIGN_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            TrayProcess.trezorService.getHardwareWalletService().requestCancel();
            throw new DeviceTimeoutException();
        }

        if (Arrays.equals(AgentConstants.SIGN_FAILED_BYTE, signature)) {
            TrayProcess.trezorService.getHardwareWalletService().requestCancel();
            throw new SignFailedException("Sign operation failed on HW.");
        }

        if (Arrays.equals(AgentConstants.SIGN_CANCELLED_BYTE, signature)) {
            throw new ActionCancelledException();
        }

        //TrayProcess.trezorService.checkoutAsyncSignData(); // null sign data        
        return signature;
    }
}
