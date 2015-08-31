package org.multibit.hd.hardware.examples.keepkey.usb.extras;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.MainNetParams;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.keepkey.clients.KeepKeyHardwareWalletClient;
import org.multibit.hd.hardware.keepkey.wallets.v1.KeepKeyV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * <p>Get a deterministic hierarchy based on the master extended public key (xpub)</p>
 * <p>Requires KeepKey V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the message sequence to get a Bitcoinj deterministic hierarchy
 * from a KeepKey that has an active wallet to enable a "watching wallet" to be created.</p>
 *
 * <h3>Only perform this example on a KeepKey that you are using for test and development!</h3>
 * <h3>Do not send funds to any addresses generated from this xpub unless you have a copy of the seed phrase written down!</h3>
 *
 * @since 0.0.1
 *  
 */
public class KeepKeyV1GetDeterministicHierarchyExample {

  private static final Logger log = LoggerFactory.getLogger(KeepKeyV1GetDeterministicHierarchyExample.class);

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
    KeepKeyV1GetDeterministicHierarchyExample example = new KeepKeyV1GetDeterministicHierarchyExample();

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

          log.debug("Wallet is present. Requesting an address...");

          // Request the extended public key for the given account
          hardwareWalletService.requestDeterministicHierarchy(
            Lists.newArrayList(
              new ChildNumber(44 | ChildNumber.HARDENED_BIT),
              ChildNumber.ZERO_HARDENED,
              ChildNumber.ZERO_HARDENED
            ));

        } else {
          log.warn("You need to have created a wallet before running this example");
          System.exit(-1);
        }

        break;
      case DETERMINISTIC_HIERARCHY:

        // Parent key should be M/44'/0'/0'
        DeterministicKey parentKey = hardwareWalletService.getContext().getDeterministicKey().get();
        log.info("Parent key path: {}", parentKey.getPathAsString());

        // Verify the deterministic hierarchy can derive child keys
        // In this case 0/0 from a parent of M/44'/0'/0'
        DeterministicHierarchy hierarchy = hardwareWalletService.getContext().getDeterministicHierarchy().get();
        DeterministicKey childKey = hierarchy.deriveChild(
          Lists.newArrayList(
            ChildNumber.ZERO
          ),
          true,
          true,
          ChildNumber.ZERO
        );

        // Calculate the address
        ECKey seedKey = ECKey.fromPublicOnly(childKey.getPubKey());
        Address walletKeyAddress = new Address(MainNetParams.get(), seedKey.getPubKeyHash());

        log.info("Path {}/0/0 has address: '{}'", parentKey.getPathAsString(), walletKeyAddress.toString());

        if ("1LqBGSKuX5yYUonjxT5qGfpUsXKYYWeabA".equals(walletKeyAddress.toString())) {
          log.warn("This corresponds to the 'abandon' wallet");
        }

        // Treat as end of example
        System.exit(0);
        break;
      case SHOW_OPERATION_FAILED:
        log.error(event.getMessage().toString());
        // Treat as end of example
        System.exit(-1);
        break;
      default:
        // Ignore
    }


  }

}
