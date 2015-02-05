package org.multibit.hd.hardware.examples.trezor.usb;

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
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * <p>Step 4 - Get a receiving address</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the message sequence to get a receiving address
 * from a Trezor that has an active wallet.</p>
 *
 * <h3>Only perform this example on a Trezor that you are using for test and development!</h3>
 * <h3>Do not send funds to the address unless you have a copy of the seed phrase written down!</h3>
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1GetAddressExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorV1GetAddressExample.class);

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
    TrezorV1GetAddressExample example = new TrezorV1GetAddressExample();

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

          log.debug("Wallet is present. Requesting an address...");

          // Request an address from the device using BIP-44 chain code:
          hardwareWalletService.requestAddress(0, KeyChain.KeyPurpose.RECEIVE_FUNDS, 0, true);

        } else {
          log.info("You need to have created a wallet before running this example");
        }

        break;
      case ADDRESS:
        Address address = ((MainNetAddress) event.getMessage().get()).getAddress().get();
        log.info("Device provided address: '{}'", address.toString());
        if ("1LqBGSKuX5yYUonjxT5qGfpUsXKYYWeabA".equals(address.toString())) {
          log.warn("This corresponds to the 'abandon' wallet");
        }
        // Treat as end of example
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
