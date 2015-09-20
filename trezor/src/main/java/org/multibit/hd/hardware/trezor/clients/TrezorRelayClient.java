package org.multibit.hd.hardware.trezor.clients;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.commons.concurrent.SafeExecutors;
import org.multibit.hd.hardware.core.HardwareWalletException;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.events.MessageEventType;
import org.multibit.hd.hardware.core.events.MessageEvents;
import org.multibit.hd.hardware.core.messages.Features;
import org.multibit.hd.hardware.trezor.utils.TrezorMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <p>Client to provide the following to applications:</p>
 * <ul>
 * <li>A client that enables communication of Trezor wire messages with a remote Trezor. Locally you
 * generate and consume Trezor messages. These are 'teleported' to a remote machine where a physical Trezor is.</li>
 * <li>The RelayClient talks to a RelayServer on a remote machine via a ServerSocket</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class TrezorRelayClient extends AbstractTrezorHardwareWalletClient {

  private static final Logger log = LoggerFactory.getLogger(TrezorRelayClient.class);

  /**
   * The location of the RelayServer (an IP address or server name
   */
  private final String relayServerLocation;

  /**
   * The port number of the RelayServer
   */
  private final int relayServerPort;

  /**
   * The socket connection to the server
   */
  private Socket socket;

  /**
   * Output to the server server
   */
  private OutputStream outputToServer;

  /**
   * Input from the server socket
   */
  private InputStream inputFromServer;

  // Provide a thread for monitoring the output from the server
  protected final ExecutorService serverMonitorService = SafeExecutors.newSingleThreadExecutor("server-monitor");


  /**
   * @param relayServerLocation The location of the RelayServer
   * @param relayServerPort     The port number of the server
   */
  public TrezorRelayClient(String relayServerLocation, int relayServerPort) {
    this.relayServerLocation = relayServerLocation;
    this.relayServerPort = relayServerPort;
  }

  @Override
  public String name() {
    return "TREZOR";
  }

  @Override
  public boolean attach() {
    try {
      socket = new Socket(relayServerLocation, relayServerPort);
      outputToServer = new BufferedOutputStream(socket.getOutputStream(), 1024);
      inputFromServer = new BufferedInputStream(socket.getInputStream(), 1024);

      log.info("Successful connection to relay server.");

      // Start the monitoring service
      monitorServer(inputFromServer);

      return true;
    } catch (UnknownHostException e) {
      log.error("Unknown host '{}'", relayServerLocation, e);
      return false;
    } catch (IOException e) {
      log.error("IO problem on host '{}'.", relayServerLocation, e);
      return false;
    }
  }

  @Override
  public void softDetach() {

    try {
      if (outputToServer != null) {
        outputToServer.close();
      }
    } catch (IOException e) {
      log.warn("Could not close output stream to server", e);
    }

    try {
      if (inputFromServer != null) {
        inputFromServer.close();
      }
    } catch (IOException e) {
      log.warn("Could not close input stream from server", e);
    }

    try {
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
      log.warn("Could not close socket", e);
    }

    log.debug("Reset endpoints");

    log.info("Detached from Trezor relay server");
  }

  @Override
  public void hardDetach() {

    log.info("Hard detach");

    softDetach();

    log.info("Hard detach completed");

  }

  @Override
  public boolean connect() {

    if (socket != null) {
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_CONNECTED, name());
      return true;
    } else {
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DISCONNECTED, name());
      return false;
    }

  }

  @Override
  public void disconnect() {
    try {

      // Close the monitoring services
      log.debug("Closing server monitor service");
      serverMonitorService.shutdownNow();
      serverMonitorService.awaitTermination(1, TimeUnit.SECONDS);

      log.debug("Closing server socket streams");
      inputFromServer.close();
      outputToServer.close();
      socket.close();

      log.info("Successful disconnection from relay server.");

    } catch (IOException ioe) {
      log.error("Do not know about host " + relayServerLocation);
    } catch (InterruptedException e) {
      log.error("Failed to terminate server monitor service");
    }
  }

  /**
   * <p>Create an executor service to poll the server output and convert them to hardware wallet events</p>
   */
  private void monitorServer(final InputStream inputFromServer) {
    // Monitor the data input stream
    serverMonitorService.submit(new Runnable() {

      @Override
      public void run() {
        while (true) {
          try {
            // Blocking read to get the client message (e.g. "Initialize") formatted as HID packets for simplicity
            log.debug("Waiting for server message...");
            MessageEvent messageFromServer = TrezorMessageUtils.parseAsHIDPackets(inputFromServer);

            MessageEvents.fireMessageEvent(messageFromServer);

          } catch (HardwareWalletException | IOException e) {
            log.error("Failed to read back from server", e);
            break;
          }
        }
      }

    });
  }

  /**
   * Send a message to an output stream
   *
   * @param message the message to serialise and send to the OutputStream
   */
  @Override
  public Optional<MessageEvent> sendMessage(Message message) {

    Preconditions.checkNotNull(message, "Message must be present");

    try {
      // Apply the message to the data output stream
      TrezorMessageUtils.writeAsHIDPackets(message, outputToServer);

    } catch (IOException e) {
      log.warn("I/O error during write. Closing socket.", e);
    }

    return Optional.absent();
  }

  @Override
  public Optional<MessageEvent> sendMessage(Message message, int duration, TimeUnit timeUnit) {

    Preconditions.checkNotNull(message, "Message must be present");

    try {
      // Apply the message to the data output stream
      TrezorMessageUtils.writeAsHIDPackets(message, outputToServer);

    } catch (IOException e) {
      log.warn("I/O error during write. Closing socket.", e);
    }

    return Optional.absent();
  }

  @Override
  public boolean verifyFeatures(Features features) {
    return true;
  }
}
