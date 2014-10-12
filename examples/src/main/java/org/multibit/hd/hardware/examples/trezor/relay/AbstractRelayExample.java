package org.multibit.hd.hardware.examples.trezor.relay;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Uninterruptibles;
import org.multibit.hd.hardware.trezor.clients.TrezorRelayClient;
import org.multibit.hd.hardware.trezor.clients.TrezorRelayServer;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * <p>Abstract base class to provide the following to relay examples:</p>
 * <ul>
 * <li>Provision of common methods to reduce boilerplate</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class AbstractRelayExample {

  protected static final Logger log = LoggerFactory.getLogger(AbstractRelayExample.class);

  protected static RelayMode mode;

  protected static TrezorRelayServer server;
  protected static TrezorRelayClient client;

  private static String serverHost = null;

  protected static void printUsage() {
    System.out.println("Usage: Build the project with 'mvn clean install' then execute from 'examples' ");
    System.out.println("       'server' to start a relay server on port " + TrezorRelayServer.DEFAULT_PORT_NUMBER + ". Run this on the machine where you plug in your Trezor.");
    System.out.println("       'client <server location>' to start a relay client. Run this on the machine where you would run your wallet software/IDE.");
    System.out.println("       'both' to start both the relay client and server locally (useful for single JVM debugging).");
  }

  /**
   * Start the relay environment
   *
   * @param args The command line arguments
   *
   * @return True if the mode is configured correctly
   */
  protected static boolean startRelayEnvironment(String[] args) throws IOException {

    // Check arguments and mode
    if (!configureMode(args)) {
      printUsage();
      return false;
    }

    // Start a RelayServer if required
    if (!configureRelayServer(args)) {
      printUsage();
      return false;
    }

    // Wait a little while for the server to start
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

    // Start a RelayClient if required
    if (!configureRelayClient(args)) {
      printUsage();
      return false;
    }

    // Must be OK to be here
    return true;
  }

  /**
   * @param args The command line arguments
   *
   * @return True if the mode is configured correctly
   */
  private static boolean configureMode(String[] args) {

    if (args == null || args.length == 0) {
      log.error("No arguments passed");
      printUsage();
      return false;
    }

    try {
      mode = RelayMode.valueOf(args[0].toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Unknown mode.");
      printUsage();
      return false;
    }

    // Must be OK to be here
    return true;
  }

  /**
   * @param args The command line arguments
   *
   * @return True if the relay server started successfully
   */
  private static boolean configureRelayServer(String[] args) {

    switch (mode) {
      case SERVER:
      case BOTH:
        // Create a Trezor V1 USB client for use by the server
        TrezorV1HidHardwareWallet hardwareWallet = new TrezorV1HidHardwareWallet(
          Optional.<Integer>absent(),
          Optional.<Integer>absent(),
          Optional.<String>absent()
        );
        try {
          server = new TrezorRelayServer(hardwareWallet, TrezorRelayServer.DEFAULT_PORT_NUMBER);
        } catch (IOException e) {
          log.error("Could not start the relay server", e);
          return false;
        }
        break;
      default:
        // Do nothing
    }

    // Must be OK to be here
    return true;
  }

  /**
   * @param args The command line arguments
   *
   * @return True if the relay client started successfully
   */
  private static boolean configureRelayClient(String[] args) throws IOException {

    switch (mode) {
      case CLIENT:
        // Determine the server host
        if (args.length <= 1 || args[1].length() == 0) {
          log.error("'client' arguments must include server host name or IP address");
          return false;
        }
        serverHost = args[1];
        break;
      case BOTH:
        // Running both server and client locally
        serverHost = "localhost";
        break;
      default:
        // Do nothing
    }

    // Create a RelayClient looking at the RelayServer
    client = new TrezorRelayClient(serverHost, TrezorRelayServer.DEFAULT_PORT_NUMBER);

    // Must be OK to be here
    return true;
  }

  public static enum RelayMode {

    SERVER,
    CLIENT,
    BOTH

    // End of enum
    ;
  }
}
