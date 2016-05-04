package com.trezoragent.sshagent;

import com.google.common.eventbus.Subscribe;
import com.trezoragent.exception.DeviceFailedException;
import com.trezoragent.exception.DeviceTimeoutException;
import com.trezoragent.exception.InvalidPinException;
import com.trezoragent.gui.PassphraseDialog;
import com.trezoragent.gui.PinPad;
import com.trezoragent.gui.TrayProcess;
import com.trezoragent.utils.AgentConstants;
import com.trezoragent.utils.AgentUtils;
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
import org.multibit.hd.hardware.core.messages.Failure;
import org.multibit.hd.hardware.core.messages.Features;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.messages.PublicKey;
import org.multibit.hd.hardware.core.messages.SignedIdentity;
import org.multibit.hd.hardware.core.utils.IdentityUtils;

/**
 * Common device (Trezor, KeepKey) listener service to handle data and provide
 * some basic state information
 *
 * @author martin.lizner
 */
public abstract class DeviceService {

    protected HardwareWalletService hardwareWalletService;
    protected HardwareWalletClient client;
    protected String deviceKey;
    byte[] signedData;
    byte[] challengeData;
    protected ReadDeviceData asyncKeyData;
    protected ReadDeviceData asyncSignData;
    private Timer timer;
    protected String deviceLabel;
    private String exceptionKey;

    public DeviceService() {

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
        Logger.getLogger(DeviceService.class.getName()).log(Level.INFO, "Received USB event: {0}", new Object[]{event.getEventType().name()});
        Logger.getLogger(DeviceService.class.getName()).log(Level.FINE, "Received USB event message: {0}", new Object[]{event.getMessage()});

        switch (event.getEventType()) {
            case SHOW_DEVICE_FAILED:
                TrayProcess.handleException(new DeviceFailedException());
                System.exit(0);
                break;

            case SHOW_DEVICE_DETACHED:
                resetCachedData();
                TrayProcess.sessionTimer.stop();
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
                            Logger.getLogger(DeviceService.class.getName()).log(Level.FINE, "Timeout when waiting for PIN.");
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
                    Logger.getLogger(DeviceService.class.getName()).log(Level.FINE, "Timeout when waiting for passphrase.");
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
                    Logger.getLogger(DeviceService.class.getName()).log(Level.FINE, "SSH Public Key: {0}", openSSHkeyNistp256);

                    setDeviceKey(openSSHkeyNistp256); // this is for swing timer - frame window to display pubkey scenario
                    getAsyncKeyData().setDeviceData(openSSHkeyNistp256); // this is for Callable.call() - ssh server asks identities before sign

                    Logger.getLogger(DeviceService.class.getName()).log(Level.INFO, "Operation {0} executed successfully", "SSH2_AGENT_GET_IDENTITIES");
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    Logger.getLogger(DeviceService.class.getName()).log(Level.SEVERE, "deviceTx FAILED");
                }

                AgentUtils.restartSessionTimer();
                break;

            case SIGNED_IDENTITY:
                // Successful identity signature
                SignedIdentity signature = (SignedIdentity) event.getMessage().get();

                signedData = signature.getSignatureBytes().get();
                Logger.getLogger(DeviceService.class.getName()).log(Level.FINE, "Signature: {0}", Utils.HEX.encode(signedData));
                getAsyncSignData().setDeviceData(signedData);

                Logger.getLogger(DeviceService.class.getName()).log(Level.INFO, "Operation {0} executed successfully", "SSH2_AGENT_SIGN_REQUEST");

                AgentUtils.restartSessionTimer(); // this is probably redundant, since get pubkey operation preceeds
                break;

            case SHOW_OPERATION_FAILED:

                getAsyncSignData().setDeviceData(AgentConstants.SIGN_FAILED_BYTE);
                getAsyncKeyData().setDeviceData(AgentConstants.GET_IDENTITIES_FAILED_STRING);

                Failure failure = (Failure) event.getMessage().get();

                switch (failure.getType()) {
                    case PIN_INVALID:
                        exceptionKey = ExceptionHandler.getErrorKeyForException(new InvalidPinException());
                        TrayProcess.createWarning(LocalizedLogger.getLocalizedMessage(exceptionKey));
                        break;
                    case ACTION_CANCELLED:
                        Logger.getLogger(DeviceService.class.getName()).log(Level.FINE, "Action cancelled.");
                        getAsyncSignData().setDeviceData(AgentConstants.SIGN_CANCELLED_BYTE); // no need to raise error, since sign fail was caused by user pressing Cancel button
                        break;
                    case PIN_CANCELLED:
                        Logger.getLogger(DeviceService.class.getName()).log(Level.FINE, "PIN cancelled.");
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

    public String getDeviceKey() {
        return deviceKey;
    }

    public void setDeviceKey(String deviceKey) {
        this.deviceKey = deviceKey;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public ReadDeviceData checkoutAsyncKeyData() {
        getAsyncKeyData().setDeviceData(null);
        return getAsyncKeyData();
    }

    public ReadDeviceData checkoutAsyncSignData() {
        getAsyncSignData().setDeviceData(null);
        return getAsyncSignData();
    }

    private void resetCachedData() {
        checkoutAsyncKeyData();
        checkoutAsyncSignData();
        setDeviceKey(null);
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
    public ReadDeviceData getAsyncKeyData() {
        return asyncKeyData;
    }

    /**
     * @return the asyncSignData
     */
    public ReadDeviceData getAsyncSignData() {
        return asyncSignData;
    }

}
