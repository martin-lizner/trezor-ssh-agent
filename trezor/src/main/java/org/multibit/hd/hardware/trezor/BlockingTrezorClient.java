package org.multibit.hd.hardware.trezor;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletSystemEvent;
import org.multibit.hd.hardware.core.messages.ProtocolMessageType;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>Client to provide the following to applications:</p>
 * <ul>
 * <li>Simple blocking accessor methods based on common use cases</li>
 * </ul>
 * <p>This is intended as a high level API to the Trezor device. Developers who need more control over the
 * responses and events are advised to study the org.multibit.hd.hardware.trezor.examples module.</p>
 * <p>Example:</p>
 * <pre>
 * // Create a socket based Trezor client with blocking methods
 * TODO Fill this in
 * BlockingTrezorClient client = new BlockingTrezorClient(trezorWallet);
 *
 * // Connect the client
 * client.connect();
 *
 * // Send a ping
 * client.ping();
 *
 * // Initialize
 * client.initialize();
 *
 * // Finish
 * client.close();
 * </pre>
 *
 * @since 0.0.1
 *  
 */
public class BlockingTrezorClient implements HardwareWalletClient {

  private static final Logger log = LoggerFactory.getLogger(BlockingTrezorClient.class);

  private final HardwareWallet trezor;
  private boolean isTrezorValid = false;

  private ExecutorService trezorEventExecutorService = Executors.newSingleThreadExecutor();
  private boolean isSessionIdValid = true;

  /**
   * Keep track of hardware wallet events to allow blocking to occur
   */
  private final BlockingQueue<HardwareWalletProtocolEvent> hardwareWalletEvents = Queues.newArrayBlockingQueue(10);

  /**
   * @param trezor The Trezor device
   */
  public BlockingTrezorClient(HardwareWallet trezor) {
    this.trezor = trezor;
  }

  @Override
  public boolean connect() {

    log.debug("Attempting to connect...");

    boolean connected = trezor.connect();
    isTrezorValid = true;

    log.debug("Connection result: {}", connected);

    return connected;

  }

  @Override
  public void disconnect() {
    isSessionIdValid = false;
    isTrezorValid = false;
    trezor.disconnect();
    trezorEventExecutorService.shutdownNow();
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> initialize() {
    return sendBlockingMessage(TrezorMessage.Initialize
      .newBuilder()
      .build(),
      2, TimeUnit.SECONDS
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> ping() {
    return sendDefaultBlockingMessage(TrezorMessage.Ping.getDefaultInstance());
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> changePin(boolean remove) {

    return sendDefaultBlockingMessage(TrezorMessage.ChangePin
      .newBuilder()
      .setRemove(remove)
      .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> wipeDevice() {
    return sendDefaultBlockingMessage(TrezorMessage.WipeDevice.getDefaultInstance());
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> firmwareErase() {
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> firmwareUpload() {
    return null;
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> getEntropy() {
    return sendDefaultBlockingMessage(TrezorMessage.GetEntropy.getDefaultInstance());
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> getPublicKey(int index, int value, Optional<String> coinName) {

    // The master public key normally takes up to 10 seconds to complete
    return sendBlockingMessage(TrezorMessage.GetPublicKey
        .newBuilder()
        .setAddressN(index, value)
        .build(),
      10, TimeUnit.SECONDS
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> loadDevice(
    String language,
    String seed,
    String pin,
    boolean passphraseProtection
  ) {

    // Define the node
    TrezorType.HDNodeType nodeType = TrezorType.HDNodeType
      .newBuilder()
      .setChainCode(ByteString.copyFromUtf8(""))
      .setChildNum(0)
      .setDepth(0)
      .setFingerprint(0)
      .build();

    // A load normally takes about 10 seconds to complete
    return sendBlockingMessage(TrezorMessage.LoadDevice
        .newBuilder()
        .setMnemonic(seed)
        .setLanguage(language)
        .setNode(nodeType)
        .setPin(pin)
        .setPassphraseProtection(passphraseProtection)
        .build(),
      15, TimeUnit.SECONDS);

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

    return sendDefaultBlockingMessage(
      TrezorMessage.ResetDevice
        .newBuilder()
        .setLanguage(language)
        .setLabel(label)
        .setDisplayRandom(displayRandom)
        .setPassphraseProtection(passphraseProtection)
        .setStrength(strength)
        .setPinProtection(pinProtection)
        .build());

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

    // TODO (GR) This is currently unmodified
    return Optional.of(tx);

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
    return sendDefaultBlockingMessage(TrezorMessage.GetAddress
      .newBuilder()
      .setAddressN(index, value)
      .setCoinName("Bitcoin")
      .build());
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> applySettings(String language, String label) {

    return sendDefaultBlockingMessage(TrezorMessage.ApplySettings
        .newBuilder()
        .setLanguage(language)
        .setLabel(label)
        .build()
    );
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

    // Add the event to the queue for blocking purposes
    hardwareWalletEvents.add(event);

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

    Preconditions.checkState(isTrezorValid, "Trezor device is not valid. Try connecting or start a new session after a disconnect.");
    Preconditions.checkState(isSessionIdValid, "An old session ID must be discarded. Create a new instance.");

    trezor.sendMessage(messageType);

    // Wait for a response
    try {
      return Optional.fromNullable(hardwareWalletEvents.poll(duration, timeUnit));
    } catch (InterruptedException e) {
      HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_FAILURE);
      return Optional.absent();
    }

  }

}
