package org.multibit.hd.hardware.examples.trezor.usb;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.Transaction;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.messages.MainNetAddress;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.examples.trezor.FakeTransactions;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1UsbHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * <p>Step 5 - Sign a fake transaction</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the message sequence to sign a transaction.</p>
 *
 * <h3>Only perform this example on a Trezor that you are using for test and development!</h3>
 * <h3>The signed tx will not be spendable so there is no point broadcasting it.</h3>
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1SignTxExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorV1SignTxExample.class);

  private HardwareWalletService hardwareWalletService;

  private Address ourReceivingAddress = null;
  private Address ourChangeAddress = null;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    TrezorV1SignTxExample example = new TrezorV1SignTxExample();

    example.executeExample();

  }

  /**
   * Execute the example
   */
  public void executeExample() {

    // Use factory to statically bind the specific hardware wallet
    TrezorV1UsbHardwareWallet wallet = HardwareWallets.newUsbInstance(
      TrezorV1UsbHardwareWallet.class,
      Optional.<Short>absent(),
      Optional.<Short>absent(),
      Optional.<String>absent()
    );

    // Wrap the hardware wallet in a suitable client to simplify message API
    HardwareWalletClient client = new TrezorHardwareWalletClient(wallet);

    // Wrap the client in a service for high level API suitable for downstream applications
    hardwareWalletService = new HardwareWalletService(client);

    // Register for the high level hardware wallet events
    HardwareWalletService.hardwareWalletEventBus.register(this);

    hardwareWalletService.start();

    // Simulate the main thread continuing with other unrelated work
    // We don't terminate main since we're using safe executors
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.HOURS);

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

          if (ourReceivingAddress == null) {
            log.debug("Request valid receiving address (chain 0)...");
            hardwareWalletService.requestAddress(0, 0, false);
            break;
          }

        } else {
          log.info("You need to have created a wallet before running this example");
        }

        break;
      case ADDRESS:
        if (ourReceivingAddress == null) {
          ourReceivingAddress = ((MainNetAddress) event.getMessage().get()).getAddress().get();
          log.info("Device provided receiving address: '{}'", ourReceivingAddress);

          // Now we require a valid change address
          log.debug("Request valid change address (chain 1)...");
          hardwareWalletService.requestAddress(1, 0, false);
          break;
        }

        if (ourChangeAddress == null) {
          ourChangeAddress = ((MainNetAddress) event.getMessage().get()).getAddress().get();
          log.info("Device provided change address: '{}'", ourChangeAddress);

          // Now we have enough information to build a transaction
          log.debug("Building a fake transaction...");

          Address merchantAddress = FakeTransactions.newMainNetAddress();
          Coin inputSatoshis = Coin.FIFTY_COINS;
          Coin outputSatoshis = Coin.COIN;

          Transaction transaction = FakeTransactions.newMainNetFakeTx(
            ourReceivingAddress,
            ourChangeAddress,
            merchantAddress,
            inputSatoshis,
            outputSatoshis
          );

          // Request an address from the device
          hardwareWalletService.signTx(transaction);

        }

        break;
      case SHOW_PIN_ENTRY:
        // Device requires the current PIN to proceed
        PinMatrixRequest request = (PinMatrixRequest) event.getMessage().get();
        Scanner keyboard = new Scanner(System.in);
        String pin;
        switch (request.getPinMatrixRequestType()) {
          case CURRENT:
            System.err.println("Recall your PIN (e.g. '1').\n" +
              "Look at the device screen and type in the numerical position of each of the digits\n" +
              "with 1 being in the bottom left and 9 being in the top right (numeric keypad style) then press ENTER.");
            pin = keyboard.next();
            hardwareWalletService.providePIN(pin);
            break;
        }
        break;

      case SHOW_OPERATION_FAILED:
        // Treat as end of example
        System.exit(0);
        break;
      default:
        // Ignore
    }


  }

}
