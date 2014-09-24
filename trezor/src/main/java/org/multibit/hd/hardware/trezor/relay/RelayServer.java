package org.multibit.hd.hardware.trezor.relay;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.multibit.hd.hardware.trezor.TrezorMessageUtils;
import org.multibit.hd.hardware.trezor.v1.TrezorV1UsbHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
public class RelayServer {

  private static final Logger log = LoggerFactory.getLogger(RelayServer.class);

  /**
   * The hardware wallet representing the locally connected hardware device
   */
  private HardwareWallet hardwareWallet;

  /**
   * The port number to use for the server socket. A RelayClient will connect to this port
   */
  private int portNumber;

  public static int DEFAULT_PORT_NUMBER = 3000;

  // The main thread the server runs on
  protected final ExecutorService serverExecutorService = Executors.newSingleThreadExecutor();

  // Provide a thread for monitoring for specialised cases
  protected final ExecutorService hardwareWalletMonitorService = Executors.newFixedThreadPool(1);

  /**
   * Create a RelayServer, wrapping a Trezor V1 USB device, exposing port 3000
   */
  public RelayServer() {
    // Create a Trezor V1 usb client
    HardwareWallet hardwareWallet = new TrezorV1UsbHardwareWallet(Optional.<Integer>absent(),
            Optional.<Integer>absent(), Optional.<String>absent());
    create(hardwareWallet, DEFAULT_PORT_NUMBER);
  }

  /**
   * Create a RelayServer using a HardwareWallet instance and the specified port
   */
  public RelayServer(HardwareWallet hardwareWallet, int portNumber) throws IOException {
    create(hardwareWallet, portNumber);

    serverExecutorService.submit(new Runnable() {
       @Override
       public void run() {
         start();
       }
     });
  }

  private void create(HardwareWallet hardwareWallet, int portNumber) {
    this.hardwareWallet = hardwareWallet;
    this.portNumber = portNumber;
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
      log.debug("To stop the server manually use ctrl-C.");
      ServerSocket serverSocket = new ServerSocket(portNumber);
      Socket clientSocket = serverSocket.accept();

      // Get the output and input streams to and from the RelayClient
      DataOutputStream outputToClient = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream(), 1024));
      DataInputStream inputFromClient = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream(), 1024));

      // Get the output and input streams to and from the hardwareWallet
      DataOutputStream outputToHardwareWallet = getHardwareWalletOutputStream();
      DataInputStream inputFromHardwareWallet = getHardwareWalletInputStream();

      Message messageFromClient = parseMessage(inputFromClient);
      log.debug("Received message from client, relaying to hardware wallet. Message = '" + messageFromClient.toString() + "'");

      sendMessage(messageFromClient, outputToHardwareWallet);
      // Send the Message to the trezor (serialising again to protobuf)

      // Monitor the input stream from the hardware wallet - this is then logged and relayed to the client
      monitorDataInputStream(inputFromHardwareWallet, outputToClient);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  /**
   * Parse the protobuf coming down the DataInputStream into a protobuf message
   * @param inputFromClient DataInputStream containing protobuf messages
   * @return the protobuf message deserialised
   */
  private Message parseMessage (DataInputStream inputFromClient) {
    // TODO
    return null;
  }

  private DataOutputStream getHardwareWalletOutputStream () {
    // TODO
    return null;
  }

  private DataInputStream getHardwareWalletInputStream () {
     // TODO
     return null;
   }

  /**
   * <p>Create an executor service to monitor the hardware wallet data input stream, raise events and relay them to the client output</p>
   */
  private void monitorDataInputStream(final DataInputStream inputFromHardwareWallet, final DataOutputStream outputToClient) {

    // Monitor the data input stream
    hardwareWalletMonitorService.submit(new Runnable() {

      @Override
      public void run() {

        while (true) {
          try {
            // Read protocol messages and fire off events (blocking)
            // TODO don't want to bother with events, simply send the protobuf of the TrezorMessage back to the client
            boolean deviceOK = hardwareWallet.adaptProtocolMessageToEvents(inputFromHardwareWallet);

            // TODO send the Events thrown by the adaptProtocolMessageToEvents as serialised protobuf to the outputToClient

            if (!deviceOK) {
              // A shutdown is imminent so best to sleep to void multiple messages
              Thread.sleep(2000);
            } else {
              // Provide a small break
              Thread.sleep(100);
            }

          } catch (InterruptedException e) {
            break;
          }
        }
      }

    });
  }

  /**
   * Send a message to an output stream
   * @param message the message to serialise and send to the OutputStream
   * @param out The outputStream to send the message to
   */
   public void sendMessage(Message message, DataOutputStream out) {

     Preconditions.checkNotNull(message, "Message must be present");

     try {
       // Apply the message to the data output stream
       TrezorMessageUtils.writeMessage(message, out);
     } catch (IOException e) {
       log.warn("I/O error during write. Closing socket.", e);

       // Must have disconnected to be here
       // TODO send a DEVICE_DISCONNECTED message back to the client
       //HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_DISCONNECTED);
     }
   }

  /**
   * Start a RelayServer wrapping a Trezor V1 USB device on the DEFAULT_PORT_NUMBER (3000)
   */
  public static void main(String[] args) {
    RelayServer relayServer = new RelayServer();

    log.debug("RelayServer started on port " + relayServer.getPortNumber() + ", wrapping the hardwareWallet '" + relayServer.getHardwareWallet().toString() + "'. Use ctrl-C to stop this server.");
    // Keep running forever
    while (true) {

    }
  }
}
