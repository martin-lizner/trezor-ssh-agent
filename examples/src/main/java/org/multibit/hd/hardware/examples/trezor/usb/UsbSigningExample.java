package org.multibit.hd.hardware.examples.trezor.usb;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.Transaction;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletSystemEvent;
import org.multibit.hd.hardware.core.messages.FakeTransactions;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.multibit.hd.hardware.trezor.BlockingTrezorClient;
import org.multibit.hd.hardware.trezor.UsbTrezorHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>Example of communicating with a Raspberry Pi Shield Trezor using USB:</p>
 * <h3>How to configure your Raspberry Pi</h3>
 * <p>Use the standard <code>rpi-serial.sh</code> script but ensure that Rpi Getty is turned off:</p>
 * <pre>
 * sudo nano /etc/inittab
 * #T0:23:respawn:/sbin/getty -L ttyAMA0 115200 vt100
 * </pre>
 * <p>This will ensure that the Shield is using the UART reach the RPi GPIO serial port with a debug socket on port
 * 2000</p>
 * <h4>*** Warning *** Do not use this mode with real private keys since it is unsafe!</h4>
 * <h3>How to run this locally</h3>
 * <p>You need to pass in the IP address of your RPi unit (use <code>ip addr</code> at the RPi terminal to find it
 * listed under <code>eth0: inet a.b.c.d</code></p>
 *
 * @since 0.0.1
 *  
 */
public class UsbSigningExample {

  private static final Logger log = LoggerFactory.getLogger(UsbSigningExample.class);

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    UsbSigningExample example = new UsbSigningExample();

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

    // Create a USB-based default Trezor client with blocking methods (quite limited)
    BlockingTrezorClient client = new BlockingTrezorClient(wallet);

    // Connect the client
    client.connect();

    // Initialize
    client.initialize();

    Thread.sleep(5000);

    // Send a ping
    client.ping();

    Thread.sleep(2000);

    // Load device (words must be on the internal list - this seed is hard coded to some Electrum addresses))
    String seed = "beyond neighbor scratch swirl embarrass doll cause also stick softly physical nice";

    client.loadDevice(
      "en",
      seed,
      "1234",
      false
    );

    // Reset device (optional)
    // byte[] entropy = client.newEntropy(256);
    // client.resetDevice(entropy);

    // Get the master public key (optional)
    //client.getMasterPublicKey();

    // Sign a transaction
    Address ourReceivingAddress = FakeTransactions.getElectrumAddressN(new int[]{0, 0});
    Address ourChangeAddress = FakeTransactions.getElectrumAddressN(new int[]{0, 1});
    Address random2Address = FakeTransactions.asMainNetAddress("1MKw8vWxvBnaBcrL2yXvZceqyRMoeG2kRn");

    Transaction tx = FakeTransactions.newMainNetFakeTx(
      ourReceivingAddress,
      ourChangeAddress,
      random2Address,
      Coin.valueOf(10L),
      Coin.valueOf(1L)
    );

    client.signTx(tx);

    log.info(tx.toString());

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
