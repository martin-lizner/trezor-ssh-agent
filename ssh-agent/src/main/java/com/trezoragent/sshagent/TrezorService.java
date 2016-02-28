package com.trezoragent.sshagent;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.trezoragent.utils.AgentConstants;
import com.trezoragent.utils.AgentUtils;
import java.security.interfaces.ECPublicKey;
import java.util.Scanner;
import org.bitcoinj.core.Utils;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.domain.Identity;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.messages.PublicKey;
import org.multibit.hd.hardware.core.messages.SignedIdentity;
import org.multibit.hd.hardware.core.utils.IdentityUtils;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author martin.lizner
 */
public class TrezorService {

    private final HardwareWalletService hardwareWalletService;
    private final HardwareWalletClient client;
    private static final Logger log = LoggerFactory.getLogger(TrezorService.class);
    private String trezorKey = "prazdny";
    byte[] signedData;
    byte[] challengeData;
    private final ReadTrezorData asyncData;

    public TrezorService() {
        TrezorV1HidHardwareWallet wallet = HardwareWallets.newUsbInstance(
                TrezorV1HidHardwareWallet.class,
                Optional.<Integer>absent(),
                Optional.<Integer>absent(),
                Optional.<String>absent()
        );

        // Wrap the hardware wallet in a suitable client to simplify message API
        client = new TrezorHardwareWalletClient(wallet);

        // Wrap the client in a service for high level API suitable for downstream applications
        hardwareWalletService = new HardwareWalletService(getClient());

        getHardwareWalletService().start();

        HardwareWalletEvents.subscribe(this);
        asyncData = new ReadTrezorData();

        log.info("Trezor Service Started");
    }

    public static TrezorService startTrezorService() {
        return new TrezorService();
    }

    public HardwareWalletService getHardwareWalletService() {
        return hardwareWalletService;
    }

    public HardwareWalletClient getClient() {
        return client;
    }

    /**
     * <p>
     * Downstream consumer applications should respond to hardware wallet
     * events</p>
     *
     * @param event The hardware wallet event indicating a state change
     */
    @Subscribe
    public void onHardwareWalletEvent(HardwareWalletEvent event) {

        log.debug("Received hardware event: '{}'.{}", event.getEventType().name(), event.getMessage());
        switch (event.getEventType()) {
            case SHOW_DEVICE_FAILED:
                // Treat as end of example
                System.exit(0);
                break;
            case SHOW_DEVICE_DETACHED:
                // Can simply wait for another device to be connected again
                break;
            case SHOW_DEVICE_READY:

                if (false && hardwareWalletService.isWalletPresent()) {

                    String challengeVisual = AgentUtils.getCurrentTimeStamp();
                    byte[] challengeHidden = challengeData;

                    // Create an identity
                    Identity identity = new Identity(AgentConstants.SSHURI, 0, challengeHidden, challengeVisual, AgentConstants.CURVE_NAME);

                    // Request an identity signature from the device
                    // The response will contain the address used
                    hardwareWalletService.signIdentity(identity);

                } else {
                    log.info("You need to have created a wallet before running this example");
                }

                break;

            case SHOW_PIN_ENTRY:
                // Device requires the current PIN to proceed
                PinMatrixRequest request = (PinMatrixRequest) event.getMessage().get();
                Scanner keyboard = new Scanner(System.in);
                String pin;
                switch (request.getPinMatrixRequestType()) {
                    case CURRENT:
                        System.err.println(
                                "Recall your PIN (e.g. '1').\n"
                                + "Look at the device screen and type in the numerical position of each of the digits\n"
                                + "with 1 being in the bottom left and 9 being in the top right (numeric keypad style) then press ENTER."
                        );
                        pin = keyboard.next();
                        hardwareWalletService.providePIN(pin);
                        break;
                }
                break;
            case PUBLIC_KEY_FOR_IDENTITY:
                // Successful identity public key
                PublicKey pubKey = (PublicKey) event.getMessage().get();

                try {
                    log.info("Raw Public Key:\n{}", (pubKey.getHdNodeType().get().getPublicKey().get()));

                    // Retrieve public key from node (not xpub)
                    ECPublicKey publicKey = IdentityUtils.getPublicKeyFromBytes(pubKey.getHdNodeType().get().getPublicKey().get());

                    // Decompress key
                    String decompressedSSHKey = IdentityUtils.decompressSSHKeyFromNistp256(publicKey);

                    // Convert key to openSSH format
                    log.info("SSH Public Key:\n{}", IdentityUtils.printOpenSSHkeyNistp256(decompressedSSHKey, null));

                    trezorKey = IdentityUtils.printOpenSSHkeyNistp256(decompressedSSHKey, null);
                    asyncData.setTrezorData(trezorKey);

                    //HardwareWalletEvents.unsubscribeAll();
                    //client.clearSession();
                    //System.exit(0);
                } catch (Exception e) {
                    log.error("deviceTx FAILED.", e);
                }

                // Must have failed to be here
                // Treat as end of example
                //System.exit(-1);
                break;

            case SIGNED_IDENTITY:
                // Successful identity signature
                SignedIdentity signature = (SignedIdentity) event.getMessage().get();

                signedData = signature.getSignatureBytes().get();
                log.info("Signature:\n{}", Utils.HEX.encode(signedData));
                asyncData.setTrezorData(signedData);

                break;

            case SHOW_OPERATION_FAILED:
             
                System.exit(-1);
                break;
            default:
            // Ignore
        }

    }

    public ReadTrezorData getAsyncData() {
        return asyncData;
    }

}
