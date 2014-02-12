package org.multibit.hd.hardware.trezor;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareEvents;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletSystemEvent;
import org.multibit.hd.hardware.core.messages.ProtocolMessageType;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.multibit.hd.hardware.core.utils.SecureErase;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.multibit.hd.hardware.trezor.protobuf.TrezorMessage;
import org.multibit.hd.hardware.trezor.protobuf.TrezorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
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
  private static final int MIN_ENTROPY = 256;

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
    boolean connected = trezor.connect();
    isTrezorValid = true;

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
      .setCoinName(ByteString.copyFromUtf8(coinName.or("Bitcoin")))
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
      .setVersion(1)
      .build();

    // A load normally takes about 10 seconds to complete
    return sendBlockingMessage(TrezorMessage.LoadDevice
      .newBuilder()
      .setMnemonic(ByteString.copyFromUtf8(seed))
      .setLanguage(ByteString.copyFromUtf8(language))
      .setNode(nodeType)
      .setPin(ByteString.copyFromUtf8(pin))
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

    return sendDefaultBlockingMessage(TrezorMessage.ResetDevice
      .newBuilder()
      .setLanguage(ByteString.copyFromUtf8(language))
      .setLabel(ByteString.copyFromUtf8(label))
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

    byte[] entropy = new byte[MIN_ENTROPY];

    int inputsCount = tx.getInputs().size();
    int outputsCount = tx.getOutputs().size();

    int inputIndex = 0;
    int outputIndex = 0;

    Preconditions.checkState(
      entropy.length >= MIN_ENTROPY,
      "Insufficient entropy for signing a transaction (256 bytes is a minimum");

    Optional<HardwareWalletProtocolEvent> event = sendDefaultBlockingMessage(TrezorMessage.SignTx
      .newBuilder()
      .setInputsCount(inputsCount)
      .setOutputsCount(outputsCount)
      .setCoinName(ByteString.copyFromUtf8("BTC"))
      .build());

    SecureErase.secureErase(entropy);

    boolean finished = false;

    // Prepare structures for signatures
    List<ByteArrayOutputStream> trezorSignatures = Lists.newArrayList();
    ByteArrayOutputStream trezorSerializedTx = new ByteArrayOutputStream();

    while (!finished) {

      // Check the response is a transaction request
      if (ProtocolMessageType.TX_REQUEST.equals(event.get().getMessageType())) {

        // Examine the response (it may contain signature information in response to a TxOutput etc)
        TrezorMessage.TxRequest txRequest = (TrezorMessage.TxRequest) event.get().getMessage();

        // Check for a serialized transaction
        if (txRequest.getSerializedTx() != null) {
          try {
            trezorSerializedTx.write(txRequest.getSerializedTx().toByteArray());
          } catch (IOException e) {
            throw new IllegalStateException(
              "Could not read serialized transaction at request index " + txRequest
                .getRequestIndex(), e);
          }
        }

        // Check for a signature
        if (txRequest.getSignedIndex() >= 0 && txRequest.getSignature() != null) {
          try {
            ByteArrayOutputStream signature = new ByteArrayOutputStream();
            signature.write(txRequest.getSignature().toByteArray());
            trezorSignatures.add(txRequest.getSignedIndex(), signature);
          } catch (IOException e) {
            throw new IllegalStateException(
              "Could not read signature for signed index " + txRequest.getSignedIndex(),
              e);
          }
        }

        // Check for completion
        if (txRequest.getRequestIndex() >= 0) {

          // Require txInput/txOutput from transaction
          switch (txRequest.getRequestType()) {
            case TXINPUT:
              // Provide the requested input
              TrezorMessage.TxInput txInput = TrezorMessageUtils.newTxInput(tx, txRequest.getRequestIndex());

              // Allow plenty of time for the signing operation
              event = sendBlockingMessage(txInput, 30, TimeUnit.SECONDS);

              // Check for a TxRequest which can be processed as part of the overall loop
              // in order to extract signature information etc
              if (ProtocolMessageType.TX_REQUEST.equals(event.get().getMessageType())) {
                continue;
              }

              log.warn("Transaction signing failed on input index {} with event type '{}'", inputIndex, event.get().getMessageType().name());

              return Optional.absent();

            case TXOUTPUT:
              // Provide the requested output
              TrezorMessage.TxOutput txOutput = TrezorMessageUtils.newTxOutput(tx, txRequest.getRequestIndex());

              // Allow plenty of time for the signing operation
              event = sendBlockingMessage(txOutput, 30, TimeUnit.SECONDS);

              // Check for a TxRequest which can be processed as part of the overall loop
              // in order to extract signature information etc
              if (ProtocolMessageType.TX_REQUEST.equals(event.get().getMessageType())) {
                continue;
              }

              log.warn("Transaction signing failed on output index {} with event type '{}'", outputIndex, event.get().getMessageType().name());

              return Optional.absent();

            default:
              throw new IllegalStateException("Unknown request type " + txRequest.getRequestType().name());
          }
        } else {

          log.info("Completed transaction signing");

          // This should be the end of building the transaction
          finished = true;

        }
      }
    }

    // Modify the original transaction to include the signatures
    for (int i = 0; i < trezorSignatures.size(); i++) {

      byte[] trezorSignature = trezorSignatures.get(i).toByteArray();

      // TODO (GR/JB) Integrate these into MBHD (requires Bitcoinj to support HD)
      Script scriptSignature = new ScriptBuilder().data(trezorSignature).build();

    }

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
      .setCoinName(ByteString.copyFromUtf8(coinName.or("Bitcoin")))
      .build());
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> applySettings(String language, String label) {

    return sendDefaultBlockingMessage(TrezorMessage.ApplySettings
      .newBuilder()
      .setLanguage(ByteString.copyFromUtf8(language))
      .setLabel(ByteString.copyFromUtf8(label))
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

    // TODO Consider a better place to put this
    if (ProtocolMessageType.BUTTON_REQUEST.equals(messageType)) {
      sendBlockingMessage(TrezorMessage.ButtonAck.getDefaultInstance(), 10, TimeUnit.SECONDS);
    }

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

  private Optional<HardwareWalletProtocolEvent> sendDefaultBlockingMessage(Message trezorMessage) {
    return sendBlockingMessage(trezorMessage, 1, TimeUnit.SECONDS);
  }

  private Optional<HardwareWalletProtocolEvent> sendBlockingMessage(Message trezorMessage, int duration, TimeUnit timeUnit) {

    Preconditions.checkState(isTrezorValid, "Trezor device is not valid. Try connecting or start a new session after a disconnect.");
    Preconditions.checkState(isSessionIdValid, "An old session ID must be discarded. Create a new instance.");

    trezor.sendMessage(trezorMessage);

    // Wait for a response
    try {
      return Optional.fromNullable(hardwareWalletEvents.poll(duration, timeUnit));
    } catch (InterruptedException e) {
      HardwareEvents.fireSystemEvent(SystemMessageType.DEVICE_FAILURE);
      return Optional.absent();
    }

  }

}
