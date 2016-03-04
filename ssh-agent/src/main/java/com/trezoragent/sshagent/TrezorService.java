package com.trezoragent.sshagent;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.trezoragent.exception.DeviceTimeoutException;
import com.trezoragent.gui.PinPad;
import com.trezoragent.gui.TrayProcess;
import com.trezoragent.utils.AgentConstants;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.Timer;
import org.bitcoinj.core.Utils;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.messages.PublicKey;
import org.multibit.hd.hardware.core.messages.SignedIdentity;
import org.multibit.hd.hardware.core.utils.IdentityUtils;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.AbstractTrezorHardwareWallet;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author martin.lizner
 */
public final class TrezorService {

    private final HardwareWalletService hardwareWalletService;
    private final HardwareWalletClient client;
    private static final Logger log = LoggerFactory.getLogger(TrezorService.class);
    private String trezorKey;
    byte[] signedData;
    byte[] challengeData;
    private final ReadTrezorData asyncKeyData;
    private final ReadTrezorData asyncSignData;
    private final AbstractTrezorHardwareWallet wallet;
    private Timer timer;

    public TrezorService() {
        this.trezorKey = null;
        wallet = HardwareWallets.newUsbInstance(
                TrezorV1HidHardwareWallet.class,
                Optional.<Integer>absent(),
                Optional.<Integer>absent(),
                Optional.<String>absent()
        );

        // Wrap the hardware wallet in a suitable client to simplify message API
        client = new TrezorHardwareWalletClient(wallet);

        // Wrap the client in a service for high level API suitable for downstream applications
        hardwareWalletService = new HardwareWalletService(client);

        getHardwareWalletService().start();

        HardwareWalletEvents.subscribe(this);

        asyncKeyData = new ReadTrezorData<String>();
        asyncSignData = new ReadTrezorData<byte[]>();

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

    public AbstractTrezorHardwareWallet getWallet() {
        return wallet;
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

                System.exit(0);
                break;
            case SHOW_DEVICE_DETACHED:
                // Can simply wait for another device to be connected again
                break;
            case SHOW_DEVICE_READY:
                break;

            case SHOW_PIN_ENTRY:
                // Device requires the current PIN to proceed

                PinMatrixRequest request = (PinMatrixRequest) event.getMessage().get();
                String pin = null;
                switch (request.getPinMatrixRequestType()) {
                    case CURRENT:

                        PinPad pinPad = new PinPad();
                        pinPad.setVisible(true);

                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Future<Object> future = executor.submit(pinPad.getPinData());

                        try {
                            pin = (String) future.get(AgentConstants.PIN_WAIT_TIMEOUT, TimeUnit.SECONDS);
                        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                            log.error("Timeout when waiting for PIN...");
                            hardwareWalletService.requestCancel();
                            pinPad.dispose();
                            TrayProcess.handleException(new DeviceTimeoutException());
                            if (timer != null && timer.isRunning()) {
                                timer.stop(); // stop swing timer since pin was cancelled and pub key frame wont be displayed
                            }
                        }

                        if (AgentConstants.PIN_CANCELLED_MSG.equals(pin)) {
                            hardwareWalletService.requestCancel();
                            if (timer != null && timer.isRunning()) {
                                timer.stop(); // stop swing timer
                            }
                            break;
                        }

                        hardwareWalletService.providePIN(pin);
                        pinPad.setVisible(false);

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

                    String openSSHkeyNistp256 = IdentityUtils.printOpenSSHkeyNistp256(decompressedSSHKey, null);
                    // Convert key to openSSH format
                    log.info("SSH Public Key:\n{}", openSSHkeyNistp256);

                    setTrezorKey(openSSHkeyNistp256); // this is for swing timer - frame window to display pubkey scenario
                    asyncKeyData.setTrezorData(openSSHkeyNistp256); // this is for Callable.call() - ssh server asks identities before sign

                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    log.error("deviceTx FAILED.", e); // TODO: unify logs
                }

                break;

            case SIGNED_IDENTITY:
                // Successful identity signature
                SignedIdentity signature = (SignedIdentity) event.getMessage().get();

                signedData = signature.getSignatureBytes().get();
                log.info("Signature:\n{}", Utils.HEX.encode(signedData));
                asyncSignData.setTrezorData(signedData);

                break;

            case SHOW_OPERATION_FAILED:

                asyncSignData.setTrezorData(AgentConstants.DEVICE_TIMEOUT_BYTE_KEY);
                break;
            default:
            // Ignore
        }
    }

    public String getTrezorKey() {
        return trezorKey;
    }

    public void setTrezorKey(String trezorKey) {
        this.trezorKey = trezorKey;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public ReadTrezorData checkoutAsyncKeyData() {
        asyncKeyData.setTrezorData(null);
        return asyncKeyData;
    }

    public ReadTrezorData checkoutAsyncSignData() {
        asyncSignData.setTrezorData(null);
        return asyncSignData;
    }

}
