package org.multibit.hd.hardware.examples.trezor.relay;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Uninterruptibles;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.trezor.relay.TrezorRelayClient;
import org.multibit.hd.hardware.trezor.relay.TrezorRelayServer;
import org.multibit.hd.hardware.trezor.v1.TrezorV1UsbHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * <p>Example of communicating with a production Trezor V1 device using a RelayServer.</p>
 * <p>This is useful as an initial verification of recognising insertion and removal of a Trezor</p>
 *
 * @since 0.0.1
 *  
 */
public class RelayToTrezorV1FeaturesExample {

  private static final Logger log = LoggerFactory.getLogger(RelayToTrezorV1FeaturesExample.class);

  private static final String SERVER_VERB = "server";
  private static final String CLIENT_VERB = "client";
  private static final String BOTH_VERB = "both";

  private static TrezorRelayServer server;
  private static TrezorRelayClient client;

  private static String mode;

  private static String serverLocation = null;

  /**
   * <p>Main entry point to the example</p>
   *
   * <p>From Maven run this using:</p>
   * <h3>Server side</h3>
   * <pre>
   * cd project_root
   * mvn clean install
   * cd examples
   * mvn clean compile exec:java -Dexec.mainClass="org.multibit.hd.hardware.examples.trezor.relay.RelayToTrezorV1FeaturesExample" -Dargs="server"
   * </pre>
   *
   * <h3>Client side</h3>
   * <pre>
   * cd project_root
   * mvn clean install
   * cd examples
   * mvn clean compile exec:java -Dexec.mainClass="org.multibit.hd.hardware.examples.trezor.relay.RelayToTrezorV1FeaturesExample" -Dexec.args="client 192.168.0.1"
   * </pre>
   *
   * @param args Use "server", "client" or "both" depending on where this example is being run
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    if (args == null || args.length == 0) {
      log.error("No arguments passed");
      printUsage();
      return;
    }

    mode = args[0].toLowerCase();

    if (!(SERVER_VERB.equals(mode) || CLIENT_VERB.equals(mode) || BOTH_VERB.equals(mode))) {
      log.error("First argument must be 'server', 'client' or 'both'");
      printUsage();
      return;
    }

    // Start a RelayServer
    if (SERVER_VERB.equals(mode) || BOTH_VERB.equals(mode)) {
      // Create a Trezor V1 usb client for use by the server
      TrezorV1UsbHardwareWallet hardwareWallet1 = new TrezorV1UsbHardwareWallet(Optional.<Integer>absent(),
        Optional.<Integer>absent(), Optional.<String>absent());
      server = new TrezorRelayServer(hardwareWallet1, TrezorRelayServer.DEFAULT_PORT_NUMBER);

    }

    // Wait a little while for the server to start
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

    // Start a RelayClient
    if (CLIENT_VERB.equals(mode) || BOTH_VERB.equals(mode)) {

      if (CLIENT_VERB.equals(mode)) {
        // Check for server location passed in
        if (args.length <= 1 || args[1].length() == 0) {
          log.error("'client' arguments must include server host name or IP address");
          printUsage();
          return;
        } else {
          serverLocation = args[1];
        }
      } else {
        // running both server and client locally
        serverLocation = "localhost";
      }

      // Create a RelayClient looking at the RelayServer
      client = new TrezorRelayClient(serverLocation, TrezorRelayServer.DEFAULT_PORT_NUMBER);

      // Register to the hardware event bus
      HardwareWalletService.hardwareEventBus.register(new RelayToTrezorV1FeaturesExample());

      executeClientExample();
    }

    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.HOURS);
  }

  private static void printUsage() {
    System.out.println("Usage: Build the project with 'mvn clean install' then execute from 'examples' ");
    System.out.println("       'server' to start a relay server on port " + TrezorRelayServer.DEFAULT_PORT_NUMBER + ". Run this on the machine where you plug in your Trezor.");
    System.out.println("       'client <server location>' to start a relay client. Run this on the machine where you would run your wallet software/IDE.");
    System.out.println("       'both' to start both the relay client and server locally (useful for single JVM debugging).");
  }

  /**
   * Exercise the RelayClient
   * This will then pass messages to the server which prods the physical Trezor device
   *
   * @throws java.io.IOException If something goes wrong
   */
  private static void executeClientExample() throws IOException {

    log.info("Attempting to connect to a production V1 Trezor over relay socket");

    // Block until a client connects or fails
    if (client.connect()) {

      log.info("Attempting basic Trezor protobuf communication");

      // Initialize
      client.initialize();

      Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);

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
