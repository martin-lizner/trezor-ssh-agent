package org.multibit.hd.hardware.examples.trezor.rpi;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Transaction;
import org.multibit.hd.hardware.trezor.core.BlockingTrezorClient;
import org.multibit.hd.hardware.trezor.core.TrezorEvent;
import org.multibit.hd.hardware.trezor.core.TrezorEventType;
import org.multibit.hd.hardware.trezor.core.protobuf.TrezorMessage;
import org.multibit.hd.hardware.trezor.core.utils.FakeTransactions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

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
public class RaspberryPiShieldUsbExample {

  private static final Logger log = LoggerFactory.getLogger(RaspberryPiShieldUsbExample.class);

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    RaspberryPiShieldUsbExample example = new RaspberryPiShieldUsbExample();

    example.executeExample();

  }

  /**
   *
   * @throws java.io.IOException If something goes wrong
   */
  public void executeExample() throws IOException, InterruptedException, AddressFormatException {

    // Create a USB-based default Trezor client with blocking methods (quite limited)
    BlockingTrezorClient client = BlockingTrezorClient.newDefaultUsbInstance(BlockingTrezorClient.newSessionId());

    // Connect the client
    client.connect();

    TrezorEvent event1 =  client.getTrezorEventQueue().poll(1, TimeUnit.SECONDS);
    log.info("Received: {} ", event1.eventType());

    if (TrezorEventType.DEVICE_DISCONNECTED.equals(event1.eventType())) {
      log.error("Device is not connected");
      System.exit(-1);
    }

    // Initialize
    client.initialize();

    Thread.sleep(5000);

    // Send a ping
    client.ping();

    Thread.sleep(2000);

    // Load device (words must be on the internal list - this seed is hard coded to some Electrum addresses))
    char[] seed = "beyond neighbor scratch swirl embarrass doll cause also stick softly physical nice".toCharArray();

    client.loadDevice(
      TrezorMessage.Algorithm.ELECTRUM,
      seed,
      false,
      "1234".getBytes(),
      false
    );

    // Reset device (optional)
    // byte[] entropy = client.newEntropy(256);
    // client.resetDevice(entropy);

    // Get the master public key (optional)
    //client.getMasterPublicKey();

    // Sign a transaction
    Address ourReceivingAddress = FakeTransactions.getElectrumAddressN(new int[]{0, 0});
    Address ourChangeAddress = FakeTransactions.getElectrumAddressN(new int[] {0,1});
    Address random2Address = FakeTransactions.asMainNetAddress("1MKw8vWxvBnaBcrL2yXvZceqyRMoeG2kRn");

    Transaction tx = FakeTransactions.newMainNetFakeTx(
      ourReceivingAddress,
      ourChangeAddress,
      random2Address,
      BigInteger.TEN,
      BigInteger.ONE
    );

    client.signTx(tx);

    log.info(tx.toString());

    log.info("Closing connections");

    // Close the connection
    client.close();

    log.info("Exiting");

    // Shutdown
    System.exit(0);

  }


}
