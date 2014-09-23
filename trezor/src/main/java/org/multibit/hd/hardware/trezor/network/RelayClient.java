package org.multibit.hd.hardware.trezor.network;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletSystemEvent;
import org.multibit.hd.hardware.core.messages.ProtocolMessageType;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
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
public class RelayClient implements HardwareWalletClient {

  private static final Logger log = LoggerFactory.getLogger(RelayClient.class);

  /**
   * The location of the RelayServer (an IP address or server name
   */
  private final String passThruServerLocation;

  /**
   * The port number of the passThruServer
   */
  private final int passThruServerPort;

  /**
   * The socket connection to the server
   */
  private Socket socket;

  /**
   * Output to the server server
   */
  private PrintWriter out;

  /**
   * Input from the server socket
   */
  private BufferedReader in;

  /**
   * The hardware wallet that is physically present on the RelayServer machine
   */
  private HardwareWallet hardwareWallet;

  /**
   * @param hardwareWallet         The hardwareWallet physically present on the RelayServer machine
   * @param passThruServerLocation The location of the RelayServer
   * @param passThruServerPort     The port number of the server
   */
  public RelayClient(HardwareWallet hardwareWallet, String passThruServerLocation, int passThruServerPort) {
    this.hardwareWallet = hardwareWallet;
    this.passThruServerLocation = passThruServerLocation;
    this.passThruServerPort = passThruServerPort;
  }

  /**
   * Connect to the remote RelayServer
   *
   * @return true if connection was successful, false otherwise
   */
  public boolean connectToServer() {
    try {
      socket = new Socket(passThruServerLocation, passThruServerPort);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//        BufferedReader stdIn =
//                new BufferedReader(
//                        new InputStreamReader(System.in));
//      String userInput;
//      while ((userInput = stdIn.readLine()) != null) {
//        out.println(userInput);
//        System.out.println("echo: " + in.readLine());
//      }
    } catch (UnknownHostException e) {
      System.err.println("Do not know about host " + passThruServerLocation);
      return false;
    } catch (IOException e) {
      System.err.println("Could not get I/O for the connection to " +
              passThruServerLocation);
      return false;
    }
    return true;
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
    } catch (IOException ioe) {
      System.err.println("Do not know about host " + passThruServerLocation);
    }
    return false;
  }

  @Override
  public boolean connect() {
    // Connect to the RelayServer and then connect to the Trezor
    // TODO Implement this
    return false;
  }

  @Override
  public void disconnect() {
    // disconnect from the Trezor and then disconnect from the RelayServer
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

  private Optional<HardwareWalletProtocolEvent> sendDefaultBlockingMessage(Message messageType) {
    return sendBlockingMessage(messageType, 1, TimeUnit.SECONDS);
  }

  private Optional<HardwareWalletProtocolEvent> sendBlockingMessage(Message messageType, int duration, TimeUnit timeUnit) {
//
//    Preconditions.checkState(isTrezorValid, "Trezor device is not valid. Try connecting or start a new session after a disconnect.");
//    Preconditions.checkState(isSessionIdValid, "An old session ID must be discarded. Create a new instance.");
//
//    trezor.sendMessage(messageType);
//
//    // Wait for a response
//    try {
//      return Optional.fromNullable(hardwareWalletEvents.poll(duration, timeUnit));
//    } catch (InterruptedException e) {
//      HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_FAILURE);
//      return Optional.absent();
//    }
//    TODO Implement this
    return null;
  }

}
