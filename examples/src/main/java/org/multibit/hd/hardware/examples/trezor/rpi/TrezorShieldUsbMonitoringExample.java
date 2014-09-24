package org.multibit.hd.hardware.examples.trezor.rpi;

import com.google.bitcoin.core.AddressFormatException;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletSystemEvent;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.shield.TrezorShieldUsbHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>Example of communicating with a Raspberry Pi emulator over a USB HID interface</p>
 * <p>This is useful when a production Trezor is not available but USB functionality must be tested</p>
 *
 * <p>You will normally have a Raspberry Pi with the Shield hardware and the trezor-emu software running.
 * The power will be provided by the USB through the Shield socket and the Ethernet cable will be attached.
 * You can then ssh on to the device and start the emulator software.</p>
 *
 * @since 0.0.1
 *  
 */
public class TrezorShieldUsbMonitoringExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorShieldUsbMonitoringExample.class);

  private boolean deviceFailed = false;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    TrezorShieldUsbMonitoringExample example = new TrezorShieldUsbMonitoringExample();

    // Subscribe to hardware wallet events
    HardwareWalletService.hardwareEventBus.register(example);

    example.executeExample();

  }

  /**
   * @throws java.io.IOException If something goes wrong
   */
  public void executeExample() throws IOException, InterruptedException, AddressFormatException {

    // Use factory to statically bind the device
    TrezorShieldUsbHardwareWallet wallet = HardwareWallets.newUsbInstance(
      TrezorShieldUsbHardwareWallet.class,
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );

    // Create a blocking Trezor client (good for demonstrations but not practical for wallets)
    TrezorHardwareWalletClient client = new TrezorHardwareWalletClient(wallet);

    // Block until a client connects or fails
    if (client.connect()) {

      log.info("Attempting basic Trezor protobuf communication");

      // Wipe
      client.wipeDevice();

      // Reset
//      client.resetDevice(
//        "english",
//        "Aardvark",
//        true,
//        true,
//        true,
//        128
//      );



      // Initialize
      client.initialize();

      // Send a ping
      client.ping();

    } else {

      log.info("Device has failed. Aborting.");

    }

    log.info("Closing connections");

    // Close the connection
    client.disconnect();

    log.info("Exiting");

    // Shutdown
    System.exit(0);

  }

  @Subscribe
  public void onHardwareWalletProtocolEvent(HardwareWalletProtocolEvent event) {


  }

  @Subscribe
  public void onHardwareWalletSystemEvent(HardwareWalletSystemEvent event) {

    switch (event.getMessageType()) {
      case DEVICE_DISCONNECTED:
        log.error("Device is not connected");
        break;
      case DEVICE_FAILURE:
        log.error("Device has failed (hardware problem)");
        deviceFailed = true;
        break;
    }

  }

}
