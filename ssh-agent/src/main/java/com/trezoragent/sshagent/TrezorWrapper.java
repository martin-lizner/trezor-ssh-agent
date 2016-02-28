package com.trezoragent.sshagent;

import com.trezoragent.struct.PublicKeyDTO;
import com.trezoragent.exception.KeyStoreLoadException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import org.multibit.hd.hardware.core.domain.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.trezoragent.utils.AgentUtils;
import com.trezoragent.utils.AgentConstants;
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

    public static List<PublicKeyDTO> getIdentities(TrezorService trezorService, Boolean stripPrefix) {
        String trezorKey = "N/A";
        List<PublicKeyDTO> idents = new ArrayList<>();

        log.info("getIdentities() wallet present: " + trezorService.getHardwareWalletService().isWalletPresent());

        trezorService.getHardwareWalletService().requestPublicKeyForIdentity(AgentConstants.SSHURI, 0, AgentConstants.CURVE_NAME, false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Object> future = executor.submit(trezorService.getAsyncData());

        try {
            trezorKey = (String) future.get(30, TimeUnit.SECONDS);
            if (stripPrefix) { // remove ecdsa-sha2... from beginning
                String[] keySplit = trezorKey.split(" ");
                if (keySplit[1] != null) {
                    trezorKey = keySplit[1];
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            log.error("Timeout when waiting for Trezor...");
            // TODO: Popup window and exit
        }

        PublicKeyDTO p = new PublicKeyDTO();
        p.setbPublicKey(DatatypeConverter.parseBase64Binary(trezorKey));
        p.setsPublicKey(trezorKey);
        p.setsComment(AgentConstants.KEY_COMMENT);
        p.setbComment(AgentConstants.KEY_COMMENT.getBytes());
        idents.add(p);

        return idents;
    }

    public static byte[] signChallenge(TrezorService trezorService, byte[] challengeHidden) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, URISyntaxException, KeyStoreLoadException, InterruptedException {
        byte[] signature = "N/A".getBytes();
        String challengeVisual = AgentUtils.getCurrentTimeStamp();

        Identity identity = new Identity(AgentConstants.SSHURI, 0, challengeHidden, challengeVisual, AgentConstants.CURVE_NAME);

        trezorService.getHardwareWalletService().signIdentity(identity);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Object> future = executor.submit(trezorService.getAsyncData());
        try {
            signature = (byte[]) future.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            log.error("Timeout when waiting for Trezor...");
            // TODO: Popup window and exit
        }

        return signature;
    }

}
