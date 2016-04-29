package com.trezoragent.sshagent;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.trezoragent.exception.DeviceFailedException;
import com.trezoragent.exception.DeviceTimeoutException;
import com.trezoragent.exception.InvalidPinException;
import com.trezoragent.gui.PassphraseDialog;
import com.trezoragent.gui.PinPad;
import com.trezoragent.gui.TrayProcess;
import com.trezoragent.utils.AgentConstants;
import com.trezoragent.utils.ExceptionHandler;
import com.trezoragent.utils.LocalizedLogger;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import org.bitcoinj.core.Utils;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.Failure;
import org.multibit.hd.hardware.core.messages.Features;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.messages.PublicKey;
import org.multibit.hd.hardware.core.messages.SignedIdentity;
import org.multibit.hd.hardware.core.utils.IdentityUtils;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.keepkey.clients.KeepKeyHardwareWalletClient;
import org.multibit.hd.hardware.keepkey.wallets.AbstractKeepKeyHardwareWallet;
import org.multibit.hd.hardware.keepkey.wallets.v1.KeepKeyV1HidHardwareWallet;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.AbstractTrezorHardwareWallet;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;

/**
 *
 * @author martin.lizner
 */
public final class TrezorService {

    private final HardwareWalletService hardwareWalletService;
    private final HardwareWalletClient client;
    private String trezorKey;
    byte[] signedData;
    byte[] challengeData;
    private final ReadTrezorData asyncKeyData;
    private final ReadTrezorData asyncSignData;
    //private final AbstractTrezorHardwareWallet wallet;
    private final AbstractKeepKeyHardwareWallet wallet;
    private Timer timer;
    private String deviceLabel;
    private String exceptionKey;

    public TrezorService() {
        this.trezorKey = null;
        /*
        wallet = HardwareWallets.newUsbInstance(
                TrezorV1HidHardwareWallet.class,
                Optional.<Integer>absent(),
                Optional.<Integer>absent(),
                Optional.<String>absent()
        );
         */
        wallet = HardwareWallets.newUsbInstance(
                KeepKeyV1HidHardwareWallet.class,
                Optional.<Integer>absent(),
                Optional.<Integer>absent(),
                Optional.<String>absent()
        );

        // Wrap the hardware wallet in a suitable client to simplify message API
        //client = new TrezorHardwareWalletClient(wallet);
        client = new KeepKeyHardwareWalletClient(wallet);

        // Wrap the client in a service for high level API suitable for downstream applications
        hardwareWalletService = new HardwareWalletService(client);

        deviceLabel = AgentConstants.DEVICE_LABEL_DEFAULT; // set default name before real one is obtained from HW

        getHardwareWalletService().start();

        HardwareWalletEvents.subscribe(this);

        asyncKeyData = new ReadTrezorData<String>();
        asyncSignData = new ReadTrezorData<byte[]>();

        Logger.getLogger(TrezorService.class.getName()).log(Level.INFO, "Trezor Service Started.");
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

    /*
    public AbstractTrezorHardwareWallet getWallet() {
        return wallet;
    }
     */
    public AbstractKeepKeyHardwareWallet getWallet() {
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
        Logger.getLogger(TrezorService.class.getName()).log(Level.INFO, "Received USB event: {0}", new Object[]{event.getEventType().name()});
        Logger.getLogger(TrezorService.class.getName()).log(Level.FINE, "Received USB event message: {0}", new Object[]{event.getMessage()});

        switch (event.getEventType()) {
            case SHOW_DEVICE_FAILED:
                TrayProcess.handleException(new DeviceFailedException());
                System.exit(0);
                break;

            case SHOW_DEVICE_DETACHED:
                resetCachedData();
                break;

            case SHOW_DEVICE_READY:
                this.deviceLabel = ((Features) event.getMessage().get()).getLabel();
                break;

            case SHOW_PIN_ENTRY:
                // Device requires the current PIN to proceed

                PinMatrixRequest request = (PinMatrixRequest) event.getMessage().get();
                String pin;
                switch (request.getPinMatrixRequestType()) {
                    case CURRENT:

                        PinPad pinPad = new PinPad();
                        pinPad.setVisible(true);

                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Future<Object> future = executor.submit(pinPad.getPinData());

                        try {
                            pin = (String) future.get(AgentConstants.PIN_WAIT_TIMEOUT, TimeUnit.SECONDS);
                        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                            Logger.getLogger(TrezorService.class.getName()).log(Level.FINE, "Timeout when waiting for PIN.");
                            hardwareWalletService.requestCancel();
                            pinPad.setVisible(false);

                            if (timer != null && timer.isRunning()) {
                                TrayProcess.handleException(new DeviceTimeoutException()); // only when called from GUI
                            }
                            break;
                        }

                        if (AgentConstants.PIN_CANCELLED_MSG.equals(pin)) {
                            hardwareWalletService.requestCancel();
                            break;
                        }

                        hardwareWalletService.providePIN(pin);
                        pinPad.setVisible(false);

                        break;
                }
                break;
            case SHOW_PASSPHRASE_ENTRY:
                // Device requires the current passphrase to proceed

                String passphrase;
                PassphraseDialog passphraseDialog = new PassphraseDialog();
                passphraseDialog.setVisible(true);

                ExecutorService passExecutor = Executors.newSingleThreadExecutor();
                Future<Object> passFuture = passExecutor.submit(passphraseDialog.getPassphraseData());

                try {
                    passphrase = (String) passFuture.get(AgentConstants.PASSPHRASE_WAIT_TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                    Logger.getLogger(TrezorService.class.getName()).log(Level.FINE, "Timeout when waiting for passphrase.");
                    hardwareWalletService.requestCancel();
                    passphraseDialog.setVisible(false);

                    if (timer != null && timer.isRunning()) {
                        TrayProcess.handleException(new DeviceTimeoutException()); // only when called from GUI
                    }
                    break;
                }

                if (AgentConstants.PASSPHRASE_CANCELLED_MSG.equals(passphrase)) {
                    hardwareWalletService.requestCancel();
                    break;
                }

                hardwareWalletService.providePassphrase(passphrase);
                passphraseDialog.setVisible(false);

                break;
            case PUBLIC_KEY_FOR_IDENTITY:
                // Successful identity public key
                PublicKey pubKey = (PublicKey) event.getMessage().get();

                try {
                    byte[] rawPub = pubKey.getHdNodeType().get().getPublicKey().get();

                    // Retrieve public key from node (not xpub)
                    ECPublicKey publicKey = IdentityUtils.getPublicKeyFromBytes(rawPub);

                    // Decompress key
                    String decompressedSSHKey = IdentityUtils.decompressSSHKeyFromNistp256(publicKey);

                    String openSSHkeyNistp256 = IdentityUtils.printOpenSSHkeyNistp256(decompressedSSHKey, null);
                    // Convert key to openSSH format
                    Logger.getLogger(TrezorService.class.getName()).log(Level.FINE, "SSH Public Key: {0}", openSSHkeyNistp256);

                    setTrezorKey(openSSHkeyNistp256); // this is for swing timer - frame window to display pubkey scenario
                    getAsyncKeyData().setTrezorData(openSSHkeyNistp256); // this is for Callable.call() - ssh server asks identities before sign

                    Logger.getLogger(TrezorService.class.getName()).log(Level.INFO, "Operation {0} executed successfully", "SSH2_AGENT_GET_IDENTITIES");
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    Logger.getLogger(TrezorService.class.getName()).log(Level.SEVERE, "deviceTx FAILED");
                }

                break;

            case SIGNED_IDENTITY:
                // Successful identity signature
                SignedIdentity signature = (SignedIdentity) event.getMessage().get();

                signedData = signature.getSignatureBytes().get();
                Logger.getLogger(TrezorService.class.getName()).log(Level.FINE, "Signature: {0}", Utils.HEX.encode(signedData));
                getAsyncSignData().setTrezorData(signedData);

                Logger.getLogger(TrezorService.class.getName()).log(Level.INFO, "Operation {0} executed successfully", "SSH2_AGENT_SIGN_REQUEST");
                break;

            case SHOW_OPERATION_FAILED:

                getAsyncSignData().setTrezorData(AgentConstants.SIGN_FAILED_BYTE);
                getAsyncKeyData().setTrezorData(AgentConstants.GET_IDENTITIES_FAILED_STRING);

                Failure failure = (Failure) event.getMessage().get();

                switch (failure.getType()) {
                    case PIN_INVALID:
                        exceptionKey = ExceptionHandler.getErrorKeyForException(new InvalidPinException());
                        TrayProcess.createWarning(LocalizedLogger.getLocalizedMessage(exceptionKey));
                        break;
                    case ACTION_CANCELLED:
                        Logger.getLogger(TrezorService.class.getName()).log(Level.FINE, "Action cancelled.");
                        getAsyncSignData().setTrezorData(AgentConstants.SIGN_CANCELLED_BYTE); // no need to raise error, since sign fail was caused by user pressing Cancel button
                        break;
                    case PIN_CANCELLED:
                        Logger.getLogger(TrezorService.class.getName()).log(Level.FINE, "PIN cancelled.");
                        break;
                }
                if (timer != null && timer.isRunning()) {
                    timer.stop(); // stop swing timer
                }

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

    public Timer getTimer() {
        return this.timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public ReadTrezorData checkoutAsyncKeyData() {
        getAsyncKeyData().setTrezorData(null);
        return getAsyncKeyData();
    }

    public ReadTrezorData checkoutAsyncSignData() {
        getAsyncSignData().setTrezorData(null);
        return getAsyncSignData();
    }

    public void resetCachedData() {
        checkoutAsyncKeyData();
        checkoutAsyncSignData();
        setTrezorKey(null);
    }

    /**
     * @return the deviceLabel
     */
    public String getDeviceLabel() {
        return deviceLabel;
    }

    /**
     * @return the asyncKeyData
     */
    public ReadTrezorData getAsyncKeyData() {
        return asyncKeyData;
    }

    /**
     * @return the asyncSignData
     */
    public ReadTrezorData getAsyncSignData() {
        return asyncSignData;
    }

}
