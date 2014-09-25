package org.multibit.hd.hardware.examples.trezor.usb;

import com.google.bitcoin.core.AddressFormatException;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletSystemEvent;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.v1.TrezorV1UsbHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>Step 1 - Verify Trezor exists on USB</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the initial verification of recognising insertion and removal of a Trezor.</p>
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1FeaturesExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorV1FeaturesExample.class);

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    TrezorV1FeaturesExample example = new TrezorV1FeaturesExample();

    // Subscribe to hardware wallet events
    HardwareWalletService.hardwareEventBus.register(example);

    example.executeExample();

  }

  /**
   * @throws java.io.IOException If something goes wrong
   */
  public void executeExample() throws IOException, InterruptedException, AddressFormatException {

    // Use factory to statically bind the device
    TrezorV1UsbHardwareWallet wallet = HardwareWallets.newUsbInstance(
      TrezorV1UsbHardwareWallet.class,
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );

    // Create a Trezor hardware wallet client
    TrezorHardwareWalletClient client = new TrezorHardwareWalletClient(wallet);

    log.info("Attempting to connect to a production V1 Trezor over USB");

    // Block until a client connects or fails
    if (client.connect()) {

      log.info("Attempting basic Trezor protobuf communication");

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
        break;
    }

  }

}
