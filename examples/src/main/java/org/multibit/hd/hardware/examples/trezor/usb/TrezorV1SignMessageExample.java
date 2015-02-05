package org.multibit.hd.hardware.examples.trezor.usb;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.KeyChain;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.MessageSignature;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Base64;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * <p>Sign a message</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the sequence to Bitcoin sign a message.</p>
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1SignMessageExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorV1SignMessageExample.class);

  private HardwareWalletService hardwareWalletService;
  private String message;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    TrezorV1SignMessageExample example = new TrezorV1SignMessageExample();

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
        message = "Hello World!";
        if (hardwareWalletService.isWalletPresent()) {

          // Request a message signature from the device
          // The response will contain the address used
          hardwareWalletService.signMessage(
            0,
            KeyChain.KeyPurpose.RECEIVE_FUNDS,
            0,
            message.getBytes()
          );

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
      case MESSAGE_SIGNATURE:
        // Successful message signature
        MessageSignature signature = (MessageSignature) event.getMessage().get();

        try {
          log.info("Signature:\n{}", Utils.HEX.encode(signature.getSignature()));

          // Verify the signature
          String base64Signature = Base64.toBase64String(signature.getSignature());

          ECKey key = ECKey.signedMessageToKey(message, base64Signature);
          Address gotAddress = key.toAddress(MainNetParams.get());

          if (gotAddress.toString().equals(signature.getAddress())) {
            log.info("Verified the signature");
            // Treat as end of example
            System.exit(0);
          }

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
