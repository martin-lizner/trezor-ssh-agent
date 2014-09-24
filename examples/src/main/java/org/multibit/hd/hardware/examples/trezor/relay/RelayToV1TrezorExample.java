package org.multibit.hd.hardware.examples.trezor.relay;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletSystemEvent;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.multibit.hd.hardware.trezor.relay.RelayClient;
import org.multibit.hd.hardware.trezor.relay.RelayServer;
import org.multibit.hd.hardware.trezor.v1.TrezorV1UsbHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * <p>Example of communicating with a production Trezor device using a RelayServer:</p>
 * <p>This is useful as an initial verification of recognising insertion and removal of a Trezor</p>
 *
 * @since 0.0.1
 *  
 */
public class RelayToV1TrezorExample {

  private static final Logger log = LoggerFactory.getLogger(RelayToV1TrezorExample.class);

  private boolean deviceFailed = false;

  private static final String SERVER_VERB = "server";
  private static final String CLIENT_VERB = "client";
  private static final String BOTH_VERB = "both";

  private static RelayServer server;
  private static RelayClient client;

  private static String mode;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    if (args == null || args.length == 0) {
      printUsage();
      return;
    }

    mode = args[0].toLowerCase();

    if (!(SERVER_VERB.equals(mode) || CLIENT_VERB.equals(mode) || BOTH_VERB.equals(mode))) {
      printUsage();
      return;
    }

    // Start a RelayServer
    if (SERVER_VERB.equals(mode) || BOTH_VERB.equals(mode)) {
      // Create a Trezor V1 usb client for use by the server
      HardwareWallet hardwareWallet1 = new TrezorV1UsbHardwareWallet(Optional.<Integer>absent(),
              Optional.<Integer>absent(), Optional.<String>absent());
      server = new RelayServer(hardwareWallet1, RelayServer.DEFAULT_PORT_NUMBER);
    }

    // Wait a little while for the server to start
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

    // Start a RelayClient
    if (CLIENT_VERB.equals(mode) || BOTH_VERB.equals(mode)) {
      String serverLocation;

      if (CLIENT_VERB.equals(mode)) {
        // Check for server location passed in
        if (args.length <= 1 || args[1].length() == 0) {
          printUsage();
          return;
        } else {
          serverLocation = args[1];
        }
      } else {
        // running both server and client locally
        serverLocation = "localhost";
      }

      // Create a Trezor V1 usb client for use by the client
      HardwareWallet hardwareWallet2 = new TrezorV1UsbHardwareWallet(Optional.<Integer>absent(),
              Optional.<Integer>absent(), Optional.<String>absent());

      // Create a RelayClient looking at the RelayServer
      client = new RelayClient(hardwareWallet2, serverLocation, RelayServer.DEFAULT_PORT_NUMBER);

      executeClientExample();
    }
  }

  private static void printUsage() {
    System.out.println("Usage: 'java org.multibit.hd.hardware.examples.trezor.relay.RelayToV1TrezorExample server' to start a relay server on port " + RelayServer.DEFAULT_PORT_NUMBER + ". Run this on the machine where you plugin your Trezor.");
    System.out.println("       'java org.multibit.hd.hardware.examples.trezor.relay.RelayToV1TrezorExample client <server location>' to start a relay client. Run this on the machine where you would run your wallet software.");
    System.out.println("       'java org.multibit.hd.hardware.examples.trezor.relay.RelayToV1TrezorExample both' to start both the relay client and server locally.");
  }

  /**
   * Exercise the RelayClient
   * This will then pass messages to the server which prods the physical Trezor device
   *
   * @throws java.io.IOException If something goes wrong
   */
  private static void executeClientExample() throws IOException{

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
    log.debug("Saw a HardwareWalletProtocolEvent: " + event);
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
