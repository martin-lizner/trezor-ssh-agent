package org.multibit.hd.hardware.trezor.relay;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletException;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.concurrent.SafeExecutors;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletSystemEvent;
import org.multibit.hd.hardware.core.messages.ProtocolMessageType;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.multibit.hd.hardware.trezor.TrezorMessageUtils;
import org.multibit.hd.hardware.trezor.v1.TrezorV1UsbHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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
  protected final ExecutorService hardwareWalletMonitorService = SafeExecutors.newSingleThreadExecutor("server-hardware-wallet");

  // Provide a thread for monitoring the output from the client
  protected final ExecutorService clientMonitorService = SafeExecutors.newSingleThreadExecutor("client-monitor");

  /**
   * Keep track of hardware wallet events to allow blocking to occur
   */
  private final BlockingQueue<HardwareWalletProtocolEvent> hardwareWalletEvents = Queues.newArrayBlockingQueue(10);


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

    HardwareWalletService.hardwareEventBus.register(this);

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
      log.debug("Starting RelayServer on port " + portNumber + ".");
      ServerSocket serverSocket = new ServerSocket(portNumber);
      Socket clientSocket = serverSocket.accept();

      // Get the output and input streams to and from the RelayClient
      DataOutputStream outputToClient = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream(), 1024));
      DataInputStream inputFromClient = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream(), 1024));

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
  private void monitorHardwareWallet(final DataOutputStream outputToClient) {
    // Monitor the data input stream
    hardwareWalletMonitorService.submit(new Runnable() {

      @Override
      public void run() {
        while (true) {
          log.debug("Monitoring the hardware wallet protobuf messages");
          Message message = hardwareWallet.readMessage();

          // Send the Message back to the client
          sendMessage(message, outputToClient);
        }
      }

    });
  }

  /**
   * <p>Create an executor service to poll the client output and relay them to hardware wallet</p>
   */
  private void monitorClient(final DataInputStream inputFromClient) {
    // Monitor the data input stream
    clientMonitorService.submit(new Runnable() {

      @Override
      public void run() {
        log.debug("Monitoring the messages the client emits . . .");
        while (true) {
          try {
            // Blocking read to get the client message (e.g. "Initialize") formatted as HID packets for simplicity
            Message messageFromClient = TrezorMessageUtils.parseAsHIDPackets(inputFromClient);

            // Send the Message to the trezor (serialising again to protobuf)
            log.debug("Writing message to hardware wallet");
            hardwareWallet.writeMessage(messageFromClient);

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

          } catch (HardwareWalletException hwe) {
            hwe.printStackTrace();
          }
        }
      }

    });
  }


  /**
   * Send a message to an output stream
   *
   * @param message the message to serialise and send to the OutputStream
   * @param out     The outputStream to send the message to
   */
  public void sendMessage(Message message, DataOutputStream out) {
    Preconditions.checkNotNull(message, "Message must be present");

    try {
      TrezorMessageUtils.writeAsHIDPackets(message, out);
    } catch (IOException e) {
      log.warn("I/O error during write. Closing socket.", e);
    }
  }

  @Subscribe
  public void onHardwareWalletProtocolEvent(HardwareWalletProtocolEvent event) {

    // Decode into a message type for use with a switch
    ProtocolMessageType messageType = event.getMessageType();

    // Protocol message

    log.debug("Received event: {}", event.getMessageType().name());
    log.debug("{}", event.getMessage().toString());

    // Add the event to the queue for blocking purposes
    hardwareWalletEvents.add(event);

  }

  @Subscribe
  public void onHardwareWalletSystemEvent(HardwareWalletSystemEvent event) {
    // System message
    log.debug("Received event: {}", event.getMessageType().name());
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
