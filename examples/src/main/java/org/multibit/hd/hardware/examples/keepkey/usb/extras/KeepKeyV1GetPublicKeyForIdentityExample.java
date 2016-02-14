package org.multibit.hd.hardware.examples.keepkey.usb.extras;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.messages.PublicKey;
import org.multibit.hd.hardware.core.utils.IdentityUtils;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.keepkey.clients.KeepKeyHardwareWalletClient;
import org.multibit.hd.hardware.keepkey.wallets.v1.KeepKeyV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * <p>Get a public key for an Identity</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the sequence to get a public key for an identity.</p>
 *
 * @since 0.8.0
 *  
 */
public class KeepKeyV1GetPublicKeyForIdentityExample {

  private static final Logger log = LoggerFactory.getLogger(KeepKeyV1GetPublicKeyForIdentityExample.class);

  private HardwareWalletService hardwareWalletService;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    KeepKeyV1GetPublicKeyForIdentityExample example = new KeepKeyV1GetPublicKeyForIdentityExample();

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
    KeepKeyV1HidHardwareWallet wallet = HardwareWallets.newUsbInstance(
      KeepKeyV1HidHardwareWallet.class,
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );

    // Wrap the hardware wallet in a suitable client to simplify message API
    HardwareWalletClient client = new KeepKeyHardwareWalletClient(wallet);

    // Wrap the client in a service for high level API suitable for downstream applications
    hardwareWalletService = new HardwareWalletService(client);

    // Register for the high level hardware wallet events
    HardwareWalletEvents.subscribe(this);

    hardwareWalletService.start();

  }

  /**
   * <p>Downstream consumer applications should respond to hardware wallet events</p>
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
          URI uri = URI.create("ssh://user@multibit.org/trezor-connect");

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
          log.info("Raw Public Key:\n{}", (pubKey.getHdNodeType().get().getPublicKey().get()));

          // Retrieve public key from node (not xpub)
          ECPublicKey publicKey = IdentityUtils.getPublicKeyFromBytes(pubKey.getHdNodeType().get().getPublicKey().get());

          // Decompress key
          String decompressedSSHKey = IdentityUtils.decompressSSHKeyFromNistp256(publicKey);

          // Convert key to openSSH format
          log.info("SSH Public Key:\n{}", IdentityUtils.printOpenSSHkeyNistp256(decompressedSSHKey, "User1"));

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

}
