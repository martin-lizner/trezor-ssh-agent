package org.multibit.hd.hardware.examples.trezor.usb.extras;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.messages.PublicKey;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.DatatypeConverter;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.ECPointUtil;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECNamedCurveSpec;

/**
 * <p>
 * Get a public key for an Identity</p>
 * <p>
 * Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>
 * This example demonstrates the sequence to get a public key for an
 * identity.</p>
 *
 * @since 0.8.0  
 */
public class TrezorV1GetPublicKeyForIdentityExample {

    private static final Logger log = LoggerFactory.getLogger(TrezorV1GetPublicKeyForIdentityExample.class);

    private HardwareWalletService hardwareWalletService;

    /**
     * <p>
     * Main entry point to the example</p>
     *
     * @param args None required
     *
     * @throws Exception If something goes wrong
     */
    public static void main(String[] args) throws Exception {

        // All the work is done in the class
        TrezorV1GetPublicKeyForIdentityExample example = new TrezorV1GetPublicKeyForIdentityExample();
        
        example.executeExample();
        
        // Simulate the main thread continuing with other unrelated work
        // We don't terminate main since we're using safe executors
        Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MINUTES);
    }

    /**
     * Execute the example
     */
    public void executeExample() {

        // Use factory to statically bind the specific hardware wallet
        TrezorV1HidHardwareWallet wallet = HardwareWallets.newUsbInstance(
                TrezorV1HidHardwareWallet.class,
                Optional.<Integer>absent(),
                Optional.<Integer>absent(),
                Optional.<String>absent()
        );

        // Wrap the hardware wallet in a suitable client to simplify message API
        HardwareWalletClient client = new TrezorHardwareWalletClient(wallet);

        // Wrap the client in a service for high level API suitable for downstream applications
        hardwareWalletService = new HardwareWalletService(client);

        // Register for the high level hardware wallet events
        HardwareWalletEvents.subscribe(this);

        hardwareWalletService.start();

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
                if (hardwareWalletService.isWalletPresent()) {

                    // Create an identity
                    URI uri = URI.create("ssh://www.seznam.cz");
                    /*
                    correct output should be:
                    ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBGkoolgyOag69SvyS3+HSkDL193XuC0b73DWKOKL1fps36S09sdGXAoewN4YytAzsiUuQ/bhJGeD75a+iB0tgKY= ssh://www.seznam.cz
                    
                     */

                    // Request an identity public key from the device (no screen support at present)
                    hardwareWalletService.requestPublicKeyForIdentity(uri, 0, "nist256p1", false);

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
                    log.info("Public key:\n{}", (pubKey.getHdNodeType().get().getPublicKey().get()));

                    ECPublicKey publicKey = getPublicKeyFromBytes(pubKey.getHdNodeType().get().getPublicKey().get());

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    writeStringToBuffer(dos, "ecdsa-sha2-nistp256");
                    writeStringToBuffer(dos, "nistp256");

                    byte[] x = publicKey.getW().getAffineX().toByteArray();
                    byte[] y = publicKey.getW().getAffineY().toByteArray();
                    byte[] octet = {(byte) 0x04};
                    
                    writeByteArrayToBuffer(dos, appendByteArrays(octet, appendByteArrays(x, y)));                    
                    log.info("SSH Key: " + DatatypeConverter.printBase64Binary(baos.toByteArray()));

                    System.exit(0);

                } catch (Exception e) {
                    log.error("deviceTx FAILED.", e);
                }

                // Must have failed to be here
                // Treat as end of example
                System.exit(-1);
                break;

            case SHOW_OPERATION_FAILED:
                // Treat as end of example
                System.exit(-1);
                break;
            default:
            // Ignore
        }

    }

    private ECPublicKey getPublicKeyFromBytes(byte[] pubKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("P-256");
        KeyFactory kf = KeyFactory.getInstance("ECDSA", new BouncyCastleProvider());
        ECNamedCurveSpec params = new ECNamedCurveSpec("P-256", spec.getCurve(), spec.getG(), spec.getN());
        ECPoint point = ECPointUtil.decodePoint(params.getCurve(), pubKey);
        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, params);
        ECPublicKey pk = (ECPublicKey) kf.generatePublic(pubKeySpec);
        return pk;
    }

    private boolean isValidSignature(byte[] pubKey, byte[] message, byte[] signature) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException, InvalidKeySpecException {
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", new BouncyCastleProvider());
        ecdsaVerify.initVerify(getPublicKeyFromBytes(pubKey));
        ecdsaVerify.update(message);
        return ecdsaVerify.verify(signature);
    }

    private void writeByteArrayToBuffer(DataOutputStream dos, byte[] data) throws IOException {
        dos.writeInt(data.length);
        dos.write(data);
    }

    private static void writeStringToBuffer(DataOutputStream dos, String str) throws UnsupportedEncodingException, IOException {
        dos.writeInt(str.getBytes("ascii").length);
        dos.write(str.getBytes("ascii"));
    }

    private static byte[] appendByteArrays(byte[] x, byte[] y) {
        byte[] xy = new byte[x.length + y.length];
        System.arraycopy(x, 0, xy, 0, x.length);
        System.arraycopy(y, 0, xy, x.length, y.length);

        return xy;
    }
}
