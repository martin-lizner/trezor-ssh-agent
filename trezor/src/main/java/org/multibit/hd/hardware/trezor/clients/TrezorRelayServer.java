package org.multibit.hd.hardware.trezor.clients;

import com.google.common.base.Optional;
import com.google.common.collect.Queues;
import com.google.common.eventbus.Subscribe;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletException;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.concurrent.SafeExecutors;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletMessageType;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.multibit.hd.hardware.trezor.utils.TrezorMessageUtils;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1UsbHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 *  <p>Server to provide the following to RelayClient:<br>
 *  <ul>
 *  <li>A RelayServer communicates with a physical Trezor device and 'teleports' the wire protocol messages to a
 * RelayClient that is located on a different machine.</li>
 * <li>You can thus connect your Trezor to, say, a Windows, machine running a RelayServer and connect to it from a different
 * machine. Communication between the RelayServer and RelayClient is done over a Socket. The format of the Socket communications is protobuf.</li>
 *  </ul>
 *  </p>
 *  
 */
public class TrezorRelayServer {

  private static final Logger log = LoggerFactory.getLogger(TrezorRelayServer.class);

  /**
   * The hardware wallet representing the locally connected hardware device
   */
  private TrezorV1UsbHardwareWallet hardwareWallet;

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
   * Keep track of hardware wallet events to allow blocking to occur
   */
  private final BlockingQueue<HardwareWalletEvent> hardwareWalletEvents = Queues.newArrayBlockingQueue(10);


  /**
   * Create a RelayServer, wrapping a Trezor V1 USB device, exposing port 3000
   */
  public TrezorRelayServer() {
    // Create a Trezor V1 usb client
    TrezorV1UsbHardwareWallet hardwareWallet = new TrezorV1UsbHardwareWallet(Optional.<Integer>absent(),
      Optional.<Integer>absent(), Optional.<String>absent());
    create(hardwareWallet, DEFAULT_PORT_NUMBER);
  }

  /**
   * Create a RelayServer using a HardwareWallet instance and the specified port
   */
  public TrezorRelayServer(TrezorV1UsbHardwareWallet hardwareWallet, int portNumber) throws IOException {
    create(hardwareWallet, portNumber);
  }

  private void create(TrezorV1UsbHardwareWallet hardwareWallet, int portNumber) {

    this.hardwareWallet = hardwareWallet;
    this.portNumber = portNumber;

    HardwareWalletService.hardwareWalletEventBus.register(this);

    serverExecutorService.submit(new Runnable() {
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

      log.debug("Waiting for TrezorRelayClient connection on port {}", portNumber);
      Socket clientSocket = serverSocket.accept();

      // Get the output and input streams to and from the RelayClient
      OutputStream outputToClient = new BufferedOutputStream(clientSocket.getOutputStream(), 1024);
      InputStream inputFromClient = new BufferedInputStream(clientSocket.getInputStream(), 1024);

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
   * <p>Create an executor service to poll the hardwareWalletEvents and relay them to the client output</p>
   */
  private void monitorHardwareWallet(final OutputStream outputToClient) {
    // Monitor the data input stream
    hardwareWalletMonitorService.submit(new Runnable() {

      @Override
      public void run() {
        while (true) {
          log.debug("Waiting for hardware wallet message...");
          Message message = hardwareWallet.readMessage();

          // Send the Message back to the client
          log.debug("Sending hardware message to client");
          writeMessage(message, outputToClient);
        }
      }

    });
  }

  /**
   * <p>Create an executor service to poll the client output and relay them to hardware wallet</p>
   */
  private void monitorClient(final InputStream inputFromClient) {
    // Monitor the data input stream
    clientMonitorService.submit(new Runnable() {

      @Override
      public void run() {
        while (true) {
          try {
            // Blocking read to get the client message (e.g. "Initialize") formatted as HID packets for simplicity
            log.debug("Waiting for client message...");

              Message messageFromClient = TrezorMessageUtils.parseAsHIDPackets(inputFromClient);

            // Send the Message to the trezor (serialising again to protobuf)
            log.debug("Writing message to hardware wallet");
            hardwareWallet.writeMessage(messageFromClient);


          } catch (HardwareWalletException | IOException e) {
            log.error("Failed in hardware wallet/client read", e);
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
  public void writeMessage(Message message, OutputStream out) {

    ByteBuffer messageBuffer = TrezorMessageUtils.formatAsHIDPackets(message);

    int packets = messageBuffer.position() / 63;
    log.debug("Writing {} packets", packets);
    messageBuffer.rewind();

    // HID requires 64 byte packets with 63 bytes of payload
    for (int i = 0; i < packets; i++) {

      byte[] buffer = new byte[64];
      buffer[0] = 63; // Length
      messageBuffer.get(buffer, 1, 63); // Payload

      // Describe the packet
      String s = "Packet [" + i + "]: ";
      for (int j = 0; j < 64; j++) {
        s += String.format(" %02x", buffer[j]);
      }

      log.debug("> Client {}", s);

      try {
        out.write(buffer);
        out.flush();
      } catch (IOException e) {
        log.error("Failed to write to client output stream.", e.getMessage());
      }

    }

  }


  @Subscribe
  public void onHardwareWalletProtocolEvent(HardwareWalletEvent event) {

    // Decode into a message type for use with a switch
    HardwareWalletMessageType messageType = event.getMessageType();

    // Protocol message

    log.debug("Received event: {}", event.getMessageType().name());
    log.debug("{}", event.getMessage().toString());

    // Add the event to the queue for blocking purposes
    hardwareWalletEvents.add(event);

  }

  /**
   * Start a RelayServer wrapping a Trezor V1 USB device on the DEFAULT_PORT_NUMBER (3000)
   */
  public static void main(String[] args) {
    TrezorRelayServer trezorRelayServer = new TrezorRelayServer();

    log.debug("RelayServer started on port " + trezorRelayServer.getPortNumber() + ", wrapping the hardwareWallet '" + trezorRelayServer.getHardwareWallet().toString() + "'. Use ctrl-C to stop this server.");
    // Keep running forever
    while (true) {

    }
  }
}
