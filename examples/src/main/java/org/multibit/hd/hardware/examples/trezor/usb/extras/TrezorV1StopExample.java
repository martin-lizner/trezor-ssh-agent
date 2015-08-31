package org.multibit.hd.hardware.examples.trezor.usb.extras;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.bitcoinj.core.AddressFormatException;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.Features;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * <p>Verify Trezor exists on USB and service can be stopped cleanly</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>Do the following:</p>
 * <ol>
 *   <li>Start with the Trezor detached</li>
 *   <li>Attach the Trezor, observe the Feature message</li>
 *   <li>Detach the Trezor, observe the service shutdown</li>
 *   <li>Type something to initiate the service restart</li>
 *   <li>Attach the Trezor, observe the Feature message</li>
 * </ol>
 *
 * <p>This example demonstrates the initial verification of recognising insertion and removal of a Trezor following by restarting
 * the service cleanly.</p>
 *
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1StopExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorV1StopExample.class);

  private HardwareWalletService hardwareWalletService;

  private int detachCount = 0;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    TrezorV1StopExample example = new TrezorV1StopExample();

    // Subscribe to hardware wallet events
    HardwareWalletEvents.subscribe(example);

    example.executeExample();

    // Simulate the main thread continuing with other unrelated work
    // We don't terminate main since we're using safe executors
    Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MINUTES);

  }

  /**
   * @throws java.io.IOException If something goes wrong
   */
  public void executeExample() throws IOException, InterruptedException, AddressFormatException {

    startHardwareWalletService();

  }

  /**
   * <p>Downstream consumer applications should respond to hardware wallet events</p>
   *
   * @param event The hardware wallet event indicating a state change
   */
  @Subscribe
  public void onHardwareWalletEvent(HardwareWalletEvent event) {

    log.debug("Received hardware event: '{}'", event.getEventType().name());

    switch (event.getEventType()) {
      case SHOW_DEVICE_FAILED:
        // Treat as end of example
        System.exit(0);
        break;
      case SHOW_DEVICE_DETACHED:
        detachCount++;
        if (detachCount == 2) {

          // Stop the service
          hardwareWalletService.stopAndWait();

          // No further events should be seen until the service is started again
          // in response to a key entry in the console

        }
        if (detachCount >= 4) {

          // Exit
          System.err.println("Stopping after " + detachCount + " detaches.");
          System.exit(0);

        }
        break;
      case SHOW_DEVICE_STOPPED:

        // Deregister for events to avoid dangling references to listeners
        HardwareWalletEvents.unsubscribe(this);

        // Create a daemon thread since we're on an event thread
        final Thread thread = new Thread(
          new Runnable() {
            @Override
            public void run() {
              System.err.println(
                "To start the service again type something then press ENTER."
              );
              Scanner keyboard = new Scanner(System.in);
              keyboard.next();

              startHardwareWalletService();
            }
          });
        thread.setDaemon(true);
        thread.setName("Second instance of service");
        thread.start();

        break;
      case SHOW_DEVICE_READY:
        // Get some information about the device
        Features features = hardwareWalletService.getContext().getFeatures().get();
        log.info("Features: {}", features);

      default:
        // Ignore
    }

  }

  private void startHardwareWalletService() {

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

}
