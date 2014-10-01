package org.multibit.hd.hardware.examples.trezor.rpi;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.Transaction;
import com.google.common.base.Preconditions;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.examples.trezor.FakeTransactions;
import org.multibit.hd.hardware.trezor.clients.AbstractTrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.shield.TrezorShieldSocketHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>Example of communicating with a Raspberry Pi Shield Trezor over a socket:</p>
 * <h3>How to configure your Raspberry Pi</h3>
 * <p>Change the standard <code>rpi-serial.sh</code> script to use the following:</p>
 * <pre>
 * python trezor/__init__.py -s -t socket -p 0.0.0.0:3000 -d -dt socket -dp 0.0.0.0:2000
 * </pre>
 * <p>This will ensure that the Shield is serving over port 3000 with a debug socket on port 2000</p>
 * <h4>*** Warning *** Do not use this mode with real private keys since it is unsafe!</h4>
 * <h3>How to run this locally</h3>
 * <p>You need to pass in the IP address of your RPi unit (use <code>ip addr</code> at the RPi terminal to find it
 * listed under <code>eth0: inet a.b.c.d</code></p>
 *
 * @since 0.0.1
 *  
 */
public class SocketSigningExample {

  private static final Logger log = LoggerFactory.getLogger(SocketSigningExample.class);

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args [0]: IP address of RPi unit (e.g. "192.168.0.1"), [1]: Port (e.g. "3000")
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    Preconditions.checkNotNull(args, "Missing arguments for RPi unit.");
    Preconditions.checkState(args.length == 2, "Required arguments [0]: host name or IP, [1]: port.");

    // All the work is done in the class
    SocketSigningExample example = new SocketSigningExample();

    // Subscribe to hardware wallet events
    HardwareWalletService.hardwareWalletEventBus.register(example);

    example.executeExample(args[0], Integer.parseInt(args[1]));

  }

  /**
   * @param host The host name or IP (e.g. "192.168.0.1")
   * @param port The port (e.g. 3000)
   *
   * @throws java.io.IOException If something goes wrong
   */
  public void executeExample(String host, int port) throws IOException, InterruptedException, AddressFormatException {

    TrezorShieldSocketHardwareWallet wallet = HardwareWallets.newSocketInstance(
      TrezorShieldSocketHardwareWallet.class,
      host,
      port
    );

    // Create a socket-based default Trezor client with blocking methods (quite limited)
    AbstractTrezorHardwareWalletClient client = new TrezorHardwareWalletClient(wallet);

    // Connect the client
    client.connect();

    // Initialize
    client.initialise();

    // Send a ping
    client.ping();

    // Get the device UUID (optional)
    // client.getUUID();

    // Get some entropy (optional)
    // client.getEntropy();

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

}
