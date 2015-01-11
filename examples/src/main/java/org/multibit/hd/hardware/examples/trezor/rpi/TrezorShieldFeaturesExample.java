package org.multibit.hd.hardware.examples.trezor.rpi;

import com.google.common.base.Optional;
import org.bitcoinj.core.AddressFormatException;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.AbstractTrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.shield.TrezorShieldUsbHardwareWallet;
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
public class TrezorShieldFeaturesExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorShieldFeaturesExample.class);

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
    TrezorShieldFeaturesExample example = new TrezorShieldFeaturesExample();

    // Subscribe to hardware wallet events
    HardwareWalletEvents.subscribe(example);

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

    // Create a Trezor hardware wallet client
    AbstractTrezorHardwareWalletClient client = new TrezorHardwareWalletClient(wallet);

    log.info("Attempting to connect to a Raspberry Pi Trezor Shield over USB");

    // Block until a client connects or fails
    if (client.connect()) {

      log.info("Attempting basic Trezor protobuf communication");

      // Initialize
      client.initialise();

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

}
