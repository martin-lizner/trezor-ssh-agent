package org.multibit.hd.hardware.keepkey.clients;

import com.google.common.base.Optional;
import com.google.common.collect.Queues;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.Message;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.multibit.commons.concurrent.SafeExecutors;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.events.MessageEventType;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.multibit.hd.hardware.keepkey.utils.KeepKeyMessageUtils;
import org.multibit.hd.hardware.keepkey.wallets.v1.KeepKeyV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *  <p>Server to provide the following to RelayClient:<br>
 *  <ul>
 *  <li>A RelayServer communicates with a physical KeepKey device and 'teleports' the wire protocol messages to a
 * RelayClient that is located on a different machine.</li>
 * <li>You can thus connect your KeepKey to, say, a Windows, machine running a RelayServer and connect to it from a different
 * machine. Communication between the RelayServer and RelayClient is done over a Socket. The format of the Socket communications is protobuf.</li>
 *  </ul>
 *  </p>
 *  
 */
public class KeepKeyRelayServer {

  private static final Logger log = LoggerFactory.getLogger(KeepKeyRelayServer.class);

  /**
   * The hardware wallet representing the locally connected hardware device
   */
  private KeepKeyV1HidHardwareWallet hardwareWallet;

  /**
   * The port number to use for the server socket. A RelayClient will connect to this port
   */
  private int portNumber;

  public static int DEFAULT_PORT_NUMBER = 3000;

  // The main thread the server runs on
  protected final ExecutorService serverExecutorService = SafeExecutors.newSingleThreadExecutor("relay-server");

  // Provide a thread for monitoring the output from the hardware wallet
  protected final ExecutorService hardwareWalletMonitorService = SafeExecutors.newSingleThreadExecutor("hardware-monitor");

  // Provide a thread for monitoring the output from the client
  protected final ExecutorService clientMonitorService = SafeExecutors.newSingleThreadExecutor("client-monitor");

  /**
   * Keep track of low level message events to allow blocking to occur
   */
  private final BlockingQueue<MessageEvent> messageEvents = Queues.newArrayBlockingQueue(10);


  /**
   * Create a RelayServer, wrapping a KeepKey V1 HID device, exposing port 3000
   */
  public KeepKeyRelayServer() {
    // Create a KeepKey V1 HID client
    KeepKeyV1HidHardwareWallet hardwareWallet = new KeepKeyV1HidHardwareWallet(
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );
    create(hardwareWallet, DEFAULT_PORT_NUMBER);
  }

  /**
   * Create a RelayServer using a HardwareWallet instance and the specified port
   */
  public KeepKeyRelayServer(KeepKeyV1HidHardwareWallet hardwareWallet, int portNumber) throws IOException {
    create(hardwareWallet, portNumber);
  }

  private void create(KeepKeyV1HidHardwareWallet hardwareWallet, int portNumber) {

    this.hardwareWallet = hardwareWallet;
    this.portNumber = portNumber;

    // Subscribe to the high level events from the client
    HardwareWalletEvents.subscribe(this);

    serverExecutorService.submit(
      new Runnable() {
        @Override
        public void run() {
          start();
        }
      });
  }

  public int getPortNumber() {
    return portNumber;
  }

  public HardwareWallet getHardwareWallet() {
    return hardwareWallet;
  }

  /**
   * Open a socket on the portNumber and listen for protobuf messages coming down the Socket.
   * Any messages received are deserialised to protobuf messages, logged and then sent to the internal
   * hardware wallet.
   * <p/>
   * Protobuf messages from the hardware wallet are deserialised, logged,  and then sent over the socket to the client
   */
  private void start() {
    try {
      ServerSocket serverSocket = new ServerSocket(portNumber);

      log.debug("Waiting for KeepKeyRelayClient connection on port {}", portNumber);
      Socket clientSocket = serverSocket.accept();

      // Get the output and input streams to and from the RelayClient
      OutputStream outputToClient = new BufferedOutputStream(clientSocket.getOutputStream(), 1024);
      InputStream inputFromClient = new BufferedInputStream(clientSocket.getInputStream(), 1024);

      log.debug("Verifying the local environment");
      if (!hardwareWallet.attach()) {
        log.error("Failed to verify local environment");
        return;
      }

      log.debug("Connecting to the local hardware wallet");
      hardwareWallet.connect();

      log.debug("Monitoring input from client");
      monitorClient(inputFromClient);

      log.debug("Monitoring input from hardware wallet");
      monitorHardwareWallet(outputToClient);

      // Once the monitoring threads have started the server waits forever
      while (true) {
      }

    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  /**
   * <p>Create an executor service to poll the messageEvents and relay them to the client output</p>
   */
  private void monitorHardwareWallet(final OutputStream outputToClient) {
    // Monitor the data input stream
    hardwareWalletMonitorService.submit(
      new Runnable() {

        @Override
        public void run() {
          while (true) {
            log.debug("Waiting for hardware wallet message...");
            Optional<MessageEvent> messageEvent = hardwareWallet.readMessage(1, TimeUnit.MINUTES);

            if (messageEvent.isPresent()) {

              if (MessageEventType.DEVICE_FAILED.equals(messageEvent.get().getEventType())) {
                // Stop reading messages on this thread for a short while to allow recovery time
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
              } else {

                // Send the Message back to the client
                log.debug("Sending raw message to client");
                writeMessage(messageEvent.get().getRawMessage().get(), outputToClient);
              }
            }
          }
        }

      });
  }

  /**
   * <p>Create an executor service to poll the client output and relay them to hardware wallet</p>
   */
  private void monitorClient(final InputStream inputFromClient) {
    // Monitor the data input stream
    clientMonitorService.submit(
      new Runnable() {

        @Override
        public void run() {
          while (true) {
            try {
              // Blocking read to get the client message (e.g. "Initialize") formatted as HID packets for simplicity
              log.debug("Waiting for client message...");

              MessageEvent messageFromClient = KeepKeyMessageUtils.parseAsHIDPackets(inputFromClient);

              // Send the Message to the keepKey (serialising again to protobuf)
              log.debug("Writing message to hardware wallet");
              hardwareWallet.writeMessage(messageFromClient.getRawMessage().get());

            } catch (Exception e) {
              log.error("Failed in hardware wallet/client read", e);
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
  @SuppressFBWarnings(value = {"SBSC_USE_STRINGBUFFER_CONCATENATION"}, justification = "Only occurs at trace")
  public void writeMessage(Message message, OutputStream out) {

    ByteBuffer messageBuffer = KeepKeyMessageUtils.formatAsHIDPackets(message);

    int packets = messageBuffer.position() / 63;
    log.debug("Writing {} packets", packets);
    messageBuffer.rewind();

    // HID requires 64 byte packets with 63 bytes of payload
    for (int i = 0; i < packets; i++) {

      byte[] buffer = new byte[64];
      buffer[0] = 63; // Length
      messageBuffer.get(buffer, 1, 63); // Payload

      if (log.isTraceEnabled()) {
        // Describe the packet
        String s = "Packet [" + i + "]: ";
        for (int j = 0; j < 64; j++) {
          s += String.format(" %02x", buffer[j]);
        }

        // There is a security risk to raising this logging level beyond trace
        log.trace("> Client {}", s);
      }

      try {
        out.write(buffer);
        out.flush();
      } catch (IOException e) {
        log.error("Failed to write to client output stream.", e.getMessage());
      }

    }

  }


  @Subscribe
  public void onHardwareWalletProtocolEvent(MessageEvent event) {

    // Decode into a message type for use with a switch
    MessageEventType eventType = event.getEventType();

    // Protocol message

    log.debug("Received event: {}", eventType.name());
    log.debug("{}", event.getMessage().toString());

    // Add the event to the queue for blocking purposes
    messageEvents.add(event);

  }

  /**
   * Start a RelayServer wrapping a KeepKey V1 USB device on the DEFAULT_PORT_NUMBER (3000)
   */
  public static void main(String[] args) {
    KeepKeyRelayServer keepKeyRelayServer = new KeepKeyRelayServer();

    log.debug("RelayServer started on port " + keepKeyRelayServer.getPortNumber() + ", wrapping the hardwareWallet '" + keepKeyRelayServer.getHardwareWallet().toString() + "'. Use ctrl-C to stop this server.");
    // Keep running forever
    while (true) {

    }
  }
}
