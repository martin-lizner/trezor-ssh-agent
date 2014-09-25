package org.multibit.hd.hardware.trezor.relay;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletSystemEvent;
import org.multibit.hd.hardware.core.messages.ProtocolMessageType;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.multibit.hd.hardware.trezor.TrezorMessageUtils;
import org.multibit.hd.hardware.trezor.v1.TrezorV1UsbHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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
public class RelayClient implements HardwareWalletClient {

  private static final Logger log = LoggerFactory.getLogger(RelayClient.class);

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
  private DataOutputStream out;

  /**
   * Input from the server socket
   */
  private DataInputStream in;

  /**
   * The hardware wallet that is physically present on the RelayServer machine
   */
  private TrezorV1UsbHardwareWallet hardwareWallet;

  /**
   * @param hardwareWallet      The hardwareWallet physically present on the RelayServer machine
   * @param relayServerLocation The location of the RelayServer
   * @param relayServerPort     The port number of the server
   */
  public RelayClient(TrezorV1UsbHardwareWallet hardwareWallet, String relayServerLocation, int relayServerPort) {
    this.hardwareWallet = hardwareWallet;
    this.relayServerLocation = relayServerLocation;
    this.relayServerPort = relayServerPort;
  }

  /**
   * Connect to the remote RelayServer
   *
   * @return true if connection was successful, false otherwise
   */
  public boolean connectToServer() {
    try {
      socket = new Socket(relayServerLocation, relayServerPort);
      out = new DataOutputStream(socket.getOutputStream());
      in = new DataInputStream(socket.getInputStream());

      return true;
    } catch (UnknownHostException e) {
      System.err.println("Do not know about host " + relayServerLocation);
      return false;
    } catch (IOException e) {
      System.err.println("Could not get I/O for the connection to " +
              relayServerLocation);
      return false;
    }
  }


  /**
   * Disconnect to the remote RelayServer
   *
   * @return true if disconnection was successful, false otherwise
   */
  public boolean disconnectFromServer() {
    try {
      in.close();
      out.close();
      socket.close();
      return true;
    } catch (IOException ioe) {
      System.err.println("Do not know about host " + relayServerLocation);
    }
    return false;
  }

  @Override
  public boolean connect() {
    // Connect to the Trezor
    Message message = TrezorMessage.ResetDevice.getDefaultInstance();
    sendMessage(message, out);
    return true;
  }

  @Override
  public void disconnect() {
    // disconnect from the Trezor
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> initialize() {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> ping() {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> changePin(boolean remove) {
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> wipeDevice() {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> firmwareErase() {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> firmwareUpload() {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> getEntropy() {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> getPublicKey(int index, int value, Optional<String> coinName) {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> loadDevice(
          String language,
          String seed,
          String pin,
          boolean passphraseProtection
  ) {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> resetDevice(
          String language,
          String label,
          boolean displayRandom,
          boolean passphraseProtection,
          boolean pinProtection,
          int strength
  ) {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> recoverDevice(String language, String label, int wordCount, boolean passphraseProtection, boolean pinProtection) {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> wordAck(String language, String label, int wordCount, boolean passphraseProtection, boolean pinProtection) {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<Transaction> signTx(Transaction tx) {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> pinMatrixAck(byte[] pin) {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> buttonAck() {
    // TODO Implement this
    return null;
  }

  public Optional<HardwareWalletProtocolEvent> cancel() {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> getAddress(int index, int value, Optional<String> coinName) {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> applySettings(String language, String label) {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> entropyAck() {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> signMessage() {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> verifyMessage(Address address, byte[] signature, String message) {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> passphraseAck() {
    // TODO Implement this
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> estimateTxSize(Transaction tx) {
    // TODO Implement this
    return null;
  }

  @Override
  public void onHardwareWalletProtocolEvent(HardwareWalletProtocolEvent event) {

    // Decode into a message type for use with a switch
    ProtocolMessageType messageType = event.getMessageType();

    // Protocol message

    log.debug("Received event: {}", event.getMessageType().name());
    log.debug("{}", event.getMessage().toString());
  }

  @Override
  public void onHardwareWalletSystemEvent(HardwareWalletSystemEvent event) {

    // Decode into a message type for use with a switch
    SystemMessageType messageType = event.getMessageType();

    // System message

    log.debug("Received event: {}", event.getMessageType().name());

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
       // Apply the message to the data output stream
       TrezorMessageUtils.writeAsHIDPackets(message, out);
     } catch (IOException e) {
       log.warn("I/O error during write. Closing socket.", e);
     }
   }
}
