package org.multibit.hd.hardware.examples.trezor.usb.step3;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.bitcoinj.core.Address;
import org.bitcoinj.wallet.KeyChain;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.MainNetAddress;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * <p>Step 3 - Wipe the device to factory defaults and securely create seed phrase on device</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the message sequence to wipe a Trezor device back to its fresh out of the box
 * state and then set it up using a seed phrase it generates itself.</p>
 *
 * <h3>Only perform this example on a Trezor that you are using for test and development!</h3>
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1WipeAndCreateWalletExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorV1WipeAndCreateWalletExample.class);

  private HardwareWalletService hardwareWalletService;

  // Some state variables to allow second use case to follow
  private boolean isWiped = false;
  private boolean isAddressReady = false;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    TrezorV1WipeAndCreateWalletExample example = new TrezorV1WipeAndCreateWalletExample();

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
        if (!isWiped) {

          // Device is not wiped so start with that

          // Force creation of the wallet (wipe then reset)
          // Select the use of PIN protection and displaying entropy on the device
          // This is the most secure way to create a wallet
          hardwareWalletService.secureCreateWallet(
            "english",
            "Aardvark",
            false,
            true,
            128
          );

        } else {

          // Device is wiped so on to getting the address

          // Request address 0'/0/0 from the device and show it on the screen
          // to ensure no trickery is taking place
          hardwareWalletService.requestAddress(
            0,
            KeyChain.KeyPurpose.RECEIVE_FUNDS,
            0,
            true
          );

        }

        break;
      case SHOW_PIN_ENTRY:
        // Determine if this is the first or second PIN entry
        PinMatrixRequest request = (PinMatrixRequest) event.getMessage().get();
        Scanner keyboard = new Scanner(System.in);
        String pin;
        switch (request.getPinMatrixRequestType()) {
          case NEW_FIRST:
            System.err.println(
              "Choose a PIN (e.g. '1' for simplicity).\n" +
                "Look at the device screen and type in the numerical position of each of the digits\n" +
                "with 1 being in the bottom left and 9 being in the top right (numeric keypad style) then press ENTER."
            );
            pin = keyboard.next();
            hardwareWalletService.providePIN(pin);
            break;
          case NEW_SECOND:
            System.err.println(
              "Recall your PIN (e.g. '1').\n" +
                "Look at the device screen once more and type in the numerical position of each of the digits\n" +
                "with 1 being in the bottom left and 9 being in the top right (numeric keypad style) then press ENTER."
            );
            pin = keyboard.next();
            hardwareWalletService.providePIN(pin);
            break;
        }
        break;
      case PROVIDE_ENTROPY:
        // Generate 256 bits of entropy (32 bytes) using the utility method
        byte[] entropy = hardwareWalletService.generateEntropy();
        // Provide it to the device
        hardwareWalletService.provideEntropy(entropy);
        break;
      case SHOW_OPERATION_SUCCEEDED:
        System.err.println("Wallet was created.");
        if (!isWiped) {
          isWiped = true;
        }
        break;
      case ADDRESS:
        Address address = ((MainNetAddress) event.getMessage().get()).getAddress().get();
        log.info("Device provided address 0'/0/0: '{}'", address.toString());

        // We're done
        System.exit(0);

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
