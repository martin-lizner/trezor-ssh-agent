package org.multibit.hd.hardware.examples.trezor.usb.extras;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.bitcoinj.core.Utils;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.domain.Identity;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.messages.SignedIdentity;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * <p>Sign an identity</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the sequence to use the Trezor Connect features to sign an identity.</p>
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1SignIdentityExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorV1SignIdentityExample.class);

  private HardwareWalletService hardwareWalletService;
  private String challengeVisual ="2015-03-23 17:39:22";
  byte[] challengeHidden = Utils.parseAsHexOrBase58("cd8552569d6e4509266ef137584d1e62c7579b5b8ed69bbafa4b864c6521e7c2");
  byte[] signature = Utils.parseAsHexOrBase58("20f2d1a42d08c3a362be49275c3ffeeaa415fc040971985548b9f910812237bb41770bf2c8d488428799fbb7e52c11f1a3404011375e4080e077e0e42ab7a5ba02");


  /*

		var hiddenChallenge_Sha = Hashes.SHA256(random_challenge);
		var visualChallenge_Sha = Hashes.SHA256(Encoding.ASCII.GetBytes(visual_challenge));

		PubKey pubKey = new PubKey("023a472219ad3327b07c18273717bb3a40b39b743756bf287fbd5fa9d263237f45");
		bool verified = pubKey.VerifyMessage(hiddenChallenge_Sha.Concat(visualChallenge_Sha).ToArray(), Encoders.Base64.EncodeData(signature));

   */

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    TrezorV1SignIdentityExample example = new TrezorV1SignIdentityExample();

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
          URI uri =URI.create("https://user@multibit.org/trezor-connect");
          Identity identity = new Identity(uri, challengeHidden, challengeVisual, null, null, 0);

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
              "Recall your PIN (e.g. '1').\n" +
                "Look at the device screen and type in the numerical position of each of the digits\n" +
                "with 1 being in the bottom left and 9 being in the top right (numeric keypad style) then press ENTER."
            );
            pin = keyboard.next();
            hardwareWalletService.providePIN(pin);
            break;
        }
        break;
      case SIGNED_IDENTITY:
        // Successful identity signature
        SignedIdentity signature = (SignedIdentity) event.getMessage().get();

        try {
          log.info("Signature:\n{}", Utils.HEX.encode(signature.getSignatureBytes().get()));

          // TODO Verify the signature against the challenge using the nist256p1 curve somehow

          // Treat as end of example
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
