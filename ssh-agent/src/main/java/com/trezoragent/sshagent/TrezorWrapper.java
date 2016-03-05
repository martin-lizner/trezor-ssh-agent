package com.trezoragent.sshagent;

import com.trezoragent.exception.DeviceTimeoutException;
import com.trezoragent.exception.GetIdentitiesFailedException;
import com.trezoragent.exception.SignFailedException;
import com.trezoragent.struct.PublicKeyDTO;
import java.util.ArrayList;
import java.util.List;
import org.multibit.hd.hardware.core.domain.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.trezoragent.utils.AgentUtils;
import com.trezoragent.utils.AgentConstants;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author martin.lizner
 */
public class TrezorWrapper {

    private static final Logger log = LoggerFactory.getLogger(TrezorWrapper.class);

    public static void getIdentitiesRequest(TrezorService trezorService) {
        trezorService.getHardwareWalletService().requestPublicKeyForIdentity(AgentConstants.SSHURI, 0, AgentConstants.CURVE_NAME, false);
    }

    public static List<PublicKeyDTO> getIdentitiesResponse(TrezorService trezorService, Boolean stripPrefix) throws DeviceTimeoutException, GetIdentitiesFailedException {
        String trezorKey;
        List<PublicKeyDTO> idents = new ArrayList<>();

        log.info("getIdentities() wallet present: " + trezorService.getHardwareWalletService().isWalletPresent());

        getIdentitiesRequest(trezorService);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(trezorService.checkoutAsyncKeyData());

        try {
            trezorKey = future.get(AgentConstants.KEY_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            log.error("Timeout when waiting for Trezor...");
            throw new DeviceTimeoutException();
        }

        if (AgentConstants.DEVICE_FAILED_STRING.equals(trezorKey)) {
            log.error("Get identities operation failed");
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

        return idents;
    }

    public static byte[] signChallenge(TrezorService trezorService, byte[] challengeHidden) throws DeviceTimeoutException, SignFailedException {
        byte[] signature;
        String challengeVisual = AgentUtils.getCurrentTimeStamp();

        Identity identity = new Identity(AgentConstants.SSHURI, 0, challengeHidden, challengeVisual, AgentConstants.CURVE_NAME);

        trezorService.getHardwareWalletService().signIdentity(identity);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<byte[]> future = executor.submit(trezorService.checkoutAsyncSignData());

        try {
            signature = future.get(AgentConstants.SIGN_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            log.error("Timeout when waiting for Trezor...");
            throw new DeviceTimeoutException();
        }

        if (Arrays.equals(AgentConstants.DEVICE_FAILED_BYTE, signature)) {
            log.error("Sign operation failed");
            throw new SignFailedException();
        }
        return signature;
    }
}
