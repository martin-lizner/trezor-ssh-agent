package org.multibit.hd.hardware.examples.trezor.rpi;

import com.google.bitcoin.core.AddressFormatException;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletSystemEvent;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.multibit.hd.hardware.trezor.BlockingTrezorClient;
import org.multibit.hd.hardware.trezor.UsbTrezorHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * <p>Example of communicating with a Raspberry Pi Shield Trezor using USB:</p>
 * <p>See the README for instructions on how to configure your RPi</p>
 *
 * @since 0.0.1
 *  
 */
public class UsbMonitoringExample {

  private static final Logger log = LoggerFactory.getLogger(UsbMonitoringExample.class);

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    UsbMonitoringExample example = new UsbMonitoringExample();

    // Subscribe to hardware wallet events
    HardwareWalletService.hardwareEventBus.register(example);

    example.executeExample();

  }

  /**
   * @throws java.io.IOException If something goes wrong
   */
  public void executeExample() throws IOException, InterruptedException, AddressFormatException {

    UsbTrezorHardwareWallet wallet = (UsbTrezorHardwareWallet) HardwareWalletService.newUsbInstance(
      UsbTrezorHardwareWallet.class.getName(),
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );

    // Create a socket-based default Trezor client with blocking methods (quite limited)
    BlockingTrezorClient client = new BlockingTrezorClient(wallet);

    // Connect the client
    while (!client.connect()) {

      Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

    }

    // Initialize
    client.initialize();

    // Send a ping
    client.ping();

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

    if (SystemMessageType.DEVICE_DISCONNECTED.equals(event.getMessageType())) {
      log.error("Device is not connected");
      System.exit(-1);
    }


  }

}
