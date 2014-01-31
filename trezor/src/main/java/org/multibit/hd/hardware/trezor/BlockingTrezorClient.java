package org.multibit.hd.hardware.trezor;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.clients.BlockingHardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;
import org.multibit.hd.hardware.core.messages.ProtocolMessageType;
import org.multibit.hd.hardware.core.utils.SecureErase;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.multibit.hd.hardware.trezor.protobuf.TrezorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
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
 * BlockingTrezorClient client = BlockingTrezorClient.newSocketInstance(host, port, BlockingTrezorClient.newSessionId());
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
public class BlockingTrezorClient implements BlockingHardwareWalletClient {

  private static final Logger log = LoggerFactory.getLogger(BlockingTrezorClient.class);
  private static final int MIN_ENTROPY = 256;

  private final HardwareWallet trezor;
  private boolean isTrezorValid = false;

  private ExecutorService trezorEventExecutorService = Executors.newSingleThreadExecutor();
  private boolean isSessionIdValid = true;
  private final ByteString sessionId;
  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * @return The session ID
   */
  public static ByteString newSessionId() {
    return ByteString.copyFrom(Longs.toByteArray(UUID.randomUUID().getLeastSignificantBits()));
  }

  /**
   * @param trezor    The Trezor device
   * @param sessionId The session ID
   */
  public BlockingTrezorClient(HardwareWallet trezor, ByteString sessionId) {
    this.trezor = trezor;
    this.sessionId = sessionId;
  }

  @Override
  public void connect() {
    trezor.connect();
    isTrezorValid = true;
  }

  @Override
  public void disconnect() {
    isSessionIdValid = false;
    isTrezorValid = false;
    trezor.disconnect();
    trezorEventExecutorService.shutdownNow();
  }

  @Override
  public HardwareWalletProtocolEvent ping() {
    return sendDefaultBlockingMessage(TrezorMessage.Ping.getDefaultInstance());
  }

  @Override
  public HardwareWalletProtocolEvent initialize() {
    return sendBlockingMessage(TrezorMessage.Initialize
      .newBuilder()
      .setSessionId(sessionId)
      .build(),
      2, TimeUnit.SECONDS
    );
  }

  @Override
  public HardwareWalletProtocolEvent getUUID() {
    return sendDefaultBlockingMessage(TrezorMessage.GetUUID.getDefaultInstance());
  }

  @Override
  public HardwareWalletProtocolEvent optAck(char[] oneTimePassword) {

    HardwareWalletProtocolEvent event = sendDefaultBlockingMessage(TrezorMessage.OtpAck
      .newBuilder()
        // TODO (GR) This String creation is a security risk (Trezor folks have been notified)
      .setOtp(new String(oneTimePassword))
      .build()
    );
    SecureErase.secureErase(oneTimePassword);

    return event;
  }

  @Override
  public HardwareWalletProtocolEvent optCancel() {
    return sendDefaultBlockingMessage(TrezorMessage.OtpCancel.getDefaultInstance());
  }

  @Override
  public HardwareWalletProtocolEvent pinAck(char[] pin) {

    HardwareWalletProtocolEvent event = sendDefaultBlockingMessage(TrezorMessage.PinAck
      .newBuilder()
        // TODO (GR) This String creation is a security risk (Trezor folks have been notified)
      .setPin(new String(pin))
      .build()
    );
    SecureErase.secureErase(pin);

    return event;
  }

  @Override
  public HardwareWalletProtocolEvent pinCancel() {
    return sendDefaultBlockingMessage(TrezorMessage.PinCancel.getDefaultInstance());
  }

  @Override
  public HardwareWalletProtocolEvent getEntropy() {
    return sendDefaultBlockingMessage(TrezorMessage.GetEntropy.getDefaultInstance());
  }

  @Override
  public HardwareWalletProtocolEvent setMaxFeeKb(long satoshisPerKb) {

    Preconditions.checkState(
      satoshisPerKb >= 0L && satoshisPerKb < 10000000L,
      "Max fee per Kb is outside a reasonable range");

    return sendDefaultBlockingMessage(TrezorMessage.SetMaxFeeKb
      .newBuilder()
      .setMaxfeeKb(satoshisPerKb)
      .build()
    );
  }

  @Override
  public HardwareWalletProtocolEvent getMasterPublicKey() {

    // The master public key normally takes up to 10 seconds to complete
    return sendBlockingMessage(
      TrezorMessage.GetMasterPublicKey.getDefaultInstance(),
      10, TimeUnit.SECONDS
    );
  }

  @Override
  public HardwareWalletProtocolEvent getAddress(int index, int value) {
    return sendDefaultBlockingMessage(TrezorMessage.GetAddress
      .newBuilder()
      .setAddressN(index, value)
      .build());
  }

  @Override
  public HardwareWalletProtocolEvent loadDevice(
    char[] seed,
    boolean useOtp,
    byte[] pin,
    boolean useSpv) {

    // TODO Default to BIP0032 (need some new addresses)
    TrezorMessage.Algorithm algorithm = TrezorMessage.Algorithm.ELECTRUM;

    // A load normally takes about 10 seconds to complete
    HardwareWalletProtocolEvent event = sendBlockingMessage(TrezorMessage.LoadDevice
      .newBuilder()
      .setAlgo(algorithm)
      .setSeed(new String(seed))
      .setOtp(useOtp)
      .setPin(ByteString.copyFrom(pin))
      .setSpv(useSpv)
      .build(),
      15, TimeUnit.SECONDS);

    SecureErase.secureErase(seed);

    return event;

  }

  @Override
  public HardwareWalletProtocolEvent resetDevice(
    byte[] entropy) {

    Preconditions.checkState(
      entropy.length >= MIN_ENTROPY,
      "Insufficient entropy for generating a new seed (256 bytes is a minimum");

    HardwareWalletProtocolEvent event = sendDefaultBlockingMessage(TrezorMessage.ResetDevice
      .newBuilder()
      .setRandom(ByteString.copyFrom(entropy))
      .build());

    SecureErase.secureErase(entropy);

    return event;

  }

  @Override
  public Optional<Transaction> signTx(Transaction tx) {

    byte[] entropy = new byte[MIN_ENTROPY];

    int inputsCount = tx.getInputs().size();
    int outputsCount = tx.getOutputs().size();

    int inputIndex = 0;
    int outputIndex = 0;

    secureRandom.nextBytes(entropy);

    Preconditions.checkState(
      entropy.length >= MIN_ENTROPY,
      "Insufficient entropy for signing a transaction (256 bytes is a minimum");

    HardwareWalletProtocolEvent event = sendDefaultBlockingMessage(TrezorMessage.SignTx
      .newBuilder()
      .setInputsCount(inputsCount)
      .setOutputsCount(outputsCount)
      .setRandom(ByteString.copyFrom(entropy))
      .build());

    SecureErase.secureErase(entropy);

    boolean finished = false;

    // Prepare structures for signatures
    List<ByteArrayOutputStream> trezorSignatures = Lists.newArrayList();
    ByteArrayOutputStream trezorSerializedTx = new ByteArrayOutputStream();

    while (!finished) {

      // Check the response is a transaction request
      if (ProtocolMessageType.TX_REQUEST.equals(event.getMessageType())) {

        // Examine the response (it may contain signature information in response to a TxOutput etc)
        TrezorMessage.TxRequest txRequest = (TrezorMessage.TxRequest) event.getMessage();

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
              if (ProtocolMessageType.TX_REQUEST.equals(event.getMessageType())) {
                continue;
              }

              log.warn("Transaction signing failed on input index {} with event type '{}'", inputIndex, event.getMessageType().name());

              return Optional.absent();

            case TXOUTPUT:
              // Provide the requested output
              TrezorMessage.TxOutput txOutput = TrezorMessageUtils.newTxOutput(tx, txRequest.getRequestIndex());

              // Allow plenty of time for the signing operation
              event = sendBlockingMessage(txOutput, 30, TimeUnit.SECONDS);

              // Check for a TxRequest which can be processed as part of the overall loop
              // in order to extract signature information etc
              if (ProtocolMessageType.TX_REQUEST.equals(event.getMessageType())) {
                continue;
              }

              log.warn("Transaction signing failed on output index {} with event type '{}'", outputIndex, event.getMessageType().name());

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

      // TODO (GR/JB) Work out how to retrofit this into Bitcoinj using AddressN co-ordinates for keys
      Script scriptSignature = new ScriptBuilder().data(trezorSignature).build();

    }

    // TODO (GR) This is currently unmodified
    return Optional.of(tx);

  }

  @Override
  public void onHardwareWalletProtocolEvent(HardwareWalletProtocolEvent event) {

    // Decode into a message type for use with a switch
    ProtocolMessageType messageType = event.getMessageType();

    // Protocol message

    log.debug("Received event: {}", event.getMessageType().name());
    log.debug("{}", event.getMessage().toString());

    if (ProtocolMessageType.BUTTON_REQUEST.equals(messageType)) {
      sendBlockingMessage(TrezorMessage.ButtonAck.getDefaultInstance(), 10, TimeUnit.SECONDS);
    }

  }

  @Override
  public byte[] newEntropy(int size) {
    byte[] entropy = new byte[size];
    secureRandom.nextBytes(entropy);
    return entropy;
  }

  private HardwareWalletProtocolEvent sendDefaultBlockingMessage(Message trezorMessage) {
    return sendBlockingMessage(trezorMessage, 1, TimeUnit.SECONDS);
  }

  private HardwareWalletProtocolEvent sendBlockingMessage(Message trezorMessage, int duration, TimeUnit timeUnit) {

    Preconditions.checkState(isTrezorValid, "Trezor device is not valid. Try connecting or start a new session after a disconnect.");
    Preconditions.checkState(isSessionIdValid, "An old session ID must be discarded. Create a new instance.");

    trezor.sendMessage(trezorMessage);

    return null;

  }

}
