package org.multibit.hd.hardware.trezor.core;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.trezor.core.events.TrezorEvents;
import org.multibit.hd.hardware.trezor.core.protobuf.MessageType;
import org.multibit.hd.hardware.trezor.core.protobuf.TrezorMessage;
import org.multibit.hd.hardware.trezor.core.utils.TrezorMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
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
public class BlockingTrezorClient implements TrezorListener {

  private static final Logger log = LoggerFactory.getLogger(BlockingTrezorClient.class);
  private static final int MIN_ENTROPY = 256;

  private final Trezor trezor;
  private boolean isTrezorValid = false;

  private BlockingQueue<TrezorEvent> trezorEventQueue;
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
   * <p>Convenience method to wrap a socket Trezor</p>
   *
   * @param host      The host (e.g. "localhost" or "192.168.0.1")
   * @param port      The port (e.g. 3000)
   * @param sessionId The session ID (typically from {@link BlockingTrezorClient#newSessionId()})
   *
   * @return A blocking Trezor client instance with a unique session ID
   */
  public static BlockingTrezorClient newSocketInstance(String host, int port, ByteString sessionId) {

    // Create a socket Trezor
    Trezor trezor = TrezorFactory.newSocketTrezor(host, port);

    BlockingTrezorClient trezorClient = new BlockingTrezorClient(trezor, sessionId);

    // Add this as the listener (sets the event queue)
    trezor.addListener(trezorClient);

    // Return the new client
    return trezorClient;

  }

  /**
   * <p>Convenience method to wrap a standard USB Trezor (the normal mode of operation)</p>
   *
   * @param sessionId The session ID (typically from {@link BlockingTrezorClient#newSessionId()})
   *
   * @return A blocking Trezor client instance with a unique session ID
   */
  public static BlockingTrezorClient newDefaultUsbInstance(ByteString sessionId) {

    // Create a USB Trezor
    Trezor trezor = TrezorFactory.newUsbTrezor(
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );

    BlockingTrezorClient trezorClient = new BlockingTrezorClient(trezor, sessionId);

    // Add this as the listener (sets the event queue)
    trezor.addListener(trezorClient);

    // Return the new client
    return trezorClient;
  }

  /**
   * <p>Convenience method to wrap a standard USB Trezor (the normal mode of operation)</p>
   *
   * @param vendorIdOptional     The vendor ID (uses default if absent)
   * @param productIdOptional    The product ID (uses default if absent)
   * @param serialNumberOptional The device serial number (accepts any if absent)
   * @param sessionId            The session ID (typically from {@link BlockingTrezorClient#newSessionId()})
   *
   * @return A blocking Trezor client instance with a unique session ID
   */
  public static BlockingTrezorClient newUsbInstance(
    Optional<Integer> vendorIdOptional,
    Optional<Integer> productIdOptional,
    Optional<String> serialNumberOptional,
    ByteString sessionId
  ) {

    // Create a USB Trezor
    Trezor trezor = TrezorFactory.newUsbTrezor(vendorIdOptional, productIdOptional, serialNumberOptional);

    BlockingTrezorClient trezorClient = new BlockingTrezorClient(trezor, sessionId);

    // Add this as the listener (sets the event queue)
    trezor.addListener(trezorClient);

    // Return the new client
    return trezorClient;
  }

  /**
   * <p>Private constructor since applications should use the static builder methods</p>
   *
   * @param trezor    The Trezor device
   * @param sessionId The session ID
   */
  private BlockingTrezorClient(Trezor trezor, ByteString sessionId) {
    this.trezor = trezor;
    this.sessionId = sessionId;
  }

  /**
   * <p>Connect to the Trezor device. No initialization takes place.</p>
   */
  public void connect() {
    trezor.connect();
    isTrezorValid = true;
  }

  /**
   * <p>Close the connection to the Trezor device. This client instance can no longer be used.</p>
   */
  public void close() {
    isSessionIdValid = false;
    isTrezorValid = false;
    trezor.close();
    trezorEventExecutorService.shutdownNow();
  }

  /**
   * <p>Send the Ping message to the device</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the device is present</li>
   * </ul>
   *
   * @return The response from the device
   */
  public TrezorEvent ping() {
    return sendDefaultBlockingMessage(TrezorMessage.Ping.getDefaultInstance());
  }

  /**
   * <p>Send the Initialize message to the device. This initiates the session using a unique session ID for this
   * client instance.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Features containing the available feature set</li>
   * </ul>
   *
   * @return The response from the device (may take up to 2s to perform this operation)
   */
  public TrezorEvent initialize() {
    return sendBlockingMessage(TrezorMessage.Initialize
      .newBuilder()
      .setSessionId(sessionId)
      .build(),
      2, TimeUnit.SECONDS
    );
  }

  /**
   * <p>Send the GetUUID message to the device. The device will respond with its unique identifier.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>UUID containing the unique identifier for the device</li>
   * </ul>
   *
   * @return The response from the device
   */
  public TrezorEvent getUUID() {
    return sendDefaultBlockingMessage(TrezorMessage.GetUUID.getDefaultInstance());
  }

  /**
   * <p>Send the OtpAck message to the device. This is normally in response to receiving an OtpRequest from
   * the device. Client software is expected to ask the user for their one-time password in a secure manner
   * and securely erase the user input.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the password was accepted</li>
   * <li>Failure if the password was rejected</li>
   * </ul>
   * <p>Please refer to <a href="http://stackoverflow
   * .com/questions/8881291/why-is-char-preferred-over-string-for-passwords/8881376#8881376">this Stack
   * Overflow answer by Jon Skeet</a> regarding the choice of <code>char[]</code> for the argument.</p>
   * <p>This method will erase the array argument in a reasonably secure manner. It is not immune to memory
   * monitoring and this password will be provided in the clear across the wire,
   * but if the attacker only has limited access to system resources it raises the bar considerably.</p>
   *
   * @param oneTimePassword The one-time password from the user (will be securely erased)
   *
   * @return The response from the device
   */
  public TrezorEvent optAck(char[] oneTimePassword) {

    TrezorEvent event = sendDefaultBlockingMessage(TrezorMessage.OtpAck
      .newBuilder()
        // TODO (GR) This String creation is a security risk (Trezor folks have been notified)
      .setOtp(new String(oneTimePassword))
      .build()
    );
    secureErase(oneTimePassword);

    return event;
  }

  /**
   * <p>Send the OtpCancel message to the device. The device will respond by cancelling its pending
   * action and clearing the screen.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the message was acknowledged</li>
   * </ul>
   *
   * @return The response from the device
   */
  public TrezorEvent optCancel() {
    return sendDefaultBlockingMessage(TrezorMessage.OtpCancel.getDefaultInstance());
  }

  /**
   * <p>Send the OtpAck message to the device. This is normally in response to receiving an PinRequest from
   * the device. Client software is expected to ask the user for their PIN in a secure manner
   * and securely erase the user input.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the password was accepted</li>
   * <li>Failure if the password was rejected</li>
   * </ul>
   *
   * @param pin The personal identification number (PIN) from the user (will be securely erased)
   *
   * @return The response from the device
   */
  public TrezorEvent pinAck(char[] pin) {

    TrezorEvent event = sendDefaultBlockingMessage(TrezorMessage.PinAck
      .newBuilder()
        // TODO (GR) This String creation is a security risk (Trezor folks have been notified)
      .setPin(new String(pin))
      .build()
    );
    secureErase(pin);

    return event;
  }

  /**
   * <p>Send the PinCancel message to the device. The device will respond by cancelling its pending
   * action and clearing the screen.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the message was acknowledged</li>
   * </ul>
   *
   * @return The response from the device
   */
  public TrezorEvent pinCancel() {
    return sendDefaultBlockingMessage(TrezorMessage.PinCancel.getDefaultInstance());
  }

  /**
   * <p>Send the GetEntropy message to the device. The device will respond by providing some random
   * data from its internal hardware random number generator.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Entropy (with data) if the operation succeeded</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response from the device
   */
  public TrezorEvent getEntropy() {
    return sendDefaultBlockingMessage(TrezorMessage.GetEntropy.getDefaultInstance());
  }

  /**
   * <p>Send the SetMaxFeeKb message to the device. The device will respond by updating its maximum
   * fee to that provided. This level is used to provide internal sanity checking during transaction signing
   * to ensure that an appropriate fee is being charged (which can vary with the exchange rate).</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the operation succeeded</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @param satoshisPerKb The number of Satoshis per Kb (with some sanity checking)
   *
   * @return The response from the device
   */
  public TrezorEvent setMaxFeeKb(long satoshisPerKb) {

    Preconditions.checkState(
      satoshisPerKb >= 0L && satoshisPerKb < 10000000L,
      "Max fee per Kb is outside a reasonable range");

    return sendDefaultBlockingMessage(TrezorMessage.SetMaxFeeKb
      .newBuilder()
      .setMaxfeeKb(satoshisPerKb)
      .build()
    );
  }

  /**
   * <p>Send the GetMasterPublicKey message to the device. The device will respond by providing the
   * master public key which can be used to generate other public keys.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>MasterPublicKey if the operation succeeded (may take up to 10 seconds)</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response from the device
   */
  public TrezorEvent getMasterPublicKey() {

    // The master public key normally takes up to 10 seconds to complete
    return sendBlockingMessage(
      TrezorMessage.GetMasterPublicKey.getDefaultInstance(),
      10, TimeUnit.SECONDS
    );
  }

  /**
   * <p>Send the GetAddress message to the device. The device will respond by providing an address calculated
   * based on the <a href="https://en.bitcoin.it/wiki/BIP_0032">BIP 0032</a> deterministic wallet approach.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Address (with data) if the operation succeeded</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @param index The index position of the generated external public key (account i from BIP 0032)
   * @param value The key pair number (k'th key from BIP 0032)
   *
   * @return The response from the device
   */
  public TrezorEvent getAddress(int index, int value) {
    return sendDefaultBlockingMessage(TrezorMessage.GetAddress
      .newBuilder()
      .setAddressN(index, value)
      .build());
  }

  /**
   * <p>Send the LoadDevice message to the device. The device will overwrite any existing private keys and replace
   * them with based on the seed value provided.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the operation succeeded (may take up to 10 seconds)</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @param algorithm The deterministic wallet algorithm (e.g. ELECTRUM, BIP32 etc)
   * @param seed      The seed value provided by the user
   * @param useOtp    True if the device should use a one-time password (OTP)
   * @param pin       The personal identification number (PIN) for quick confirmations (will be securely erased)
   * @param useSpv    True if the device should use Simplified Payment Verification (SPV) from the main computer to
   *                  verify transactions
   *
   * @return The response from the device
   */
  public TrezorEvent loadDevice(
    TrezorMessage.Algorithm algorithm,
    char[] seed,
    boolean useOtp,
    byte[] pin,
    boolean useSpv) {

    // A load normally takes about 10 seconds to complete
    TrezorEvent event = sendBlockingMessage(TrezorMessage.LoadDevice
      .newBuilder()
      .setAlgo(algorithm)
      .setSeed(new String(seed))
      .setOtp(useOtp)
      .setPin(ByteString.copyFrom(pin))
      .setSpv(useSpv)
      .build(),
      15, TimeUnit.SECONDS);

    secureErase(seed);

    return event;

  }

  /**
   * <p>Send the ResetDevice message to the device. The device will perform a full reset, generating a new seed
   * and asking the user for new settings.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the operation succeeded</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @param entropy Additional entropy for use when generating the new seed. Use at least 256 bytes from <a
   *                href="https://en.wikipedia.org/wiki/Urandom">dev/random</a>. The array will be securely erased
   *                after use.
   *
   * @return The response from the device
   */
  public TrezorEvent resetDevice(
    byte[] entropy) {

    Preconditions.checkState(
      entropy.length >= MIN_ENTROPY,
      "Insufficient entropy for generating a new seed (256 bytes is a minimum");

    TrezorEvent event = sendDefaultBlockingMessage(TrezorMessage.ResetDevice
      .newBuilder()
      .setRandom(ByteString.copyFrom(entropy))
      .build());

    secureErase(entropy);

    return event;

  }

  /**
   * <p>Send the SignTx message to the device. Behind the scenes the device will response with a series of TxRequests
   * in order to  build up the overall transaction. This client takes care of all the chatter leaving a final
   * response condition.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the operation succeeded</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @param tx The Bitcoinj transaction providing all the necessary information (will be modified)
   *
   * @return The signed transaction from the device (if present)
   */
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

    TrezorEvent event = sendDefaultBlockingMessage(TrezorMessage.SignTx
      .newBuilder()
      .setInputsCount(inputsCount)
      .setOutputsCount(outputsCount)
      .setRandom(ByteString.copyFrom(entropy))
      .build());

    secureErase(entropy);

    boolean finished = false;

    // Prepare structures for signatures
    List<ByteArrayOutputStream> trezorSignatures = Lists.newArrayList();
    ByteArrayOutputStream trezorSerializedTx = new ByteArrayOutputStream();

    while (!finished) {

      // Check the response is a transaction request
      if (TrezorEventType.PROTOCOL_MESSAGE.equals(event.eventType())) {
        if (MessageType.TX_REQUEST.equals(event.protocolMessageType().get())) {

          // Examine the response (it may contain signature information in response to a TxOutput etc)
          TrezorMessage.TxRequest txRequest = (TrezorMessage.TxRequest) event.protocolMessage().get();

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

                if (event.eventType().equals(TrezorEventType.PROTOCOL_MESSAGE)) {

                  // Check for a TxRequest which can be processed as part of the overall loop
                  // in order to extract signature information etc
                  if (MessageType.TX_REQUEST.equals(event.protocolMessageType().get())) {
                    continue;
                  }

                }

                log.warn("Transaction signing failed on input index {} with event type '{}'", inputIndex, event.eventType().name());

                return Optional.absent();

              case TXOUTPUT:
                // Provide the requested output
                TrezorMessage.TxOutput txOutput = TrezorMessageUtils.newTxOutput(tx, txRequest.getRequestIndex());

                // Allow plenty of time for the signing operation
                event = sendBlockingMessage(txOutput, 30, TimeUnit.SECONDS);

                if (event.eventType().equals(TrezorEventType.PROTOCOL_MESSAGE)) {

                  // Check for a TxRequest which can be processed as part of the overall loop
                  // in order to extract signature information etc
                  if (MessageType.TX_REQUEST.equals(event.protocolMessageType().get())) {
                    continue;
                  }
                }

                log.warn("Transaction signing failed on output index {} with event type '{}'", outputIndex, event.eventType().name());

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

  /**
   * <p>Securely erase the given array.</p>
   * <p>The technique used ensures that JIT will not optimise the unused erasure away.</p>
   * <p>Please refer to <a href="http://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords/8881376#8881376">this Stack
   * Overflow answer by Jon Skeet</a> regarding the choice of <code>char[]</code> for the argument.</p>
   * <p>This method will erase the array argument in a reasonably secure manner. It is not immune to memory
   * monitoring and this data may be provided in the clear across the wire to the Trezor device. The approach
   * is valid since if the attacker only has limited access to system resources it raises the bar considerably.</p>
   *
   * @param value The array to be erased
   */
  public void secureErase(char[] value) {
    synchronized (BlockingTrezorClient.class) {
      int fakeSum = 0;
      for (int i = 0; i < value.length; i++) {
        value[i] = '\0';
        fakeSum += (int) value[i];
      }
      if (fakeSum == System.currentTimeMillis()) {
        throw new IllegalStateException("Could not securely erase the char[]");
      }
    }
  }

  /**
   * <p>Securely erase the given array.</p>
   * <p>The technique used ensures that JIT will not optimise the unused erasure away.</p>
   * <p>Please refer to <a href="http://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords/8881376#8881376">this Stack
   * Overflow answer by Jon Skeet</a> regarding the choice of <code>char[]</code> for the argument.</p>
   * <p>This method will erase the array argument in a reasonably secure manner. It is not immune to memory
   * monitoring and this data may be provided in the clear across the wire to the Trezor device. The approach
   * is valid since if the attacker only has limited access to system resources it raises the bar considerably.</p>
   *
   * @param value The array to be erased
   */
  public void secureErase(byte[] value) {
    synchronized (BlockingTrezorClient.class) {
      int fakeSum = 0;
      for (int i = 0; i < value.length; i++) {
        value[i] = '\0';
        fakeSum += (int) value[i];
      }
      if (fakeSum == System.currentTimeMillis()) {
        throw new IllegalStateException("Could not securely erase the byte[]");
      }
    }
  }

  /**
   * <p>Convenience method for synchronous communication with the device using the default timeout settings</p>
   *
   * @param trezorMessage The Trezor message
   *
   * @return The Trezor event
   *
   * @throws IllegalStateException If anything goes wrong
   */
  private TrezorEvent sendDefaultBlockingMessage(Message trezorMessage) {
    return sendBlockingMessage(trezorMessage, 1, TimeUnit.SECONDS);
  }

  /**
   * <p>Blocking method for synchronous communication with the device</p>
   *
   * @param trezorMessage The Trezor message
   *
   * @return The Trezor event
   *
   * @throws IllegalStateException If anything goes wrong
   */
  private TrezorEvent sendBlockingMessage(Message trezorMessage, int duration, TimeUnit timeUnit) {

    Preconditions.checkState(isTrezorValid, "Trezor device is not valid. Try connecting or start a new session after a disconnect.");
    Preconditions.checkState(isSessionIdValid, "An old session ID must be discarded. Create a new instance.");

    try {

      // Check for any new events
      TrezorEvent event = getTrezorEventQueue().poll(10, TimeUnit.MILLISECONDS);
      if (event != null) {
        // Spontaneous event has arrived
        handleTrezorEvent(event);
      }

      if (!isTrezorValid) {
        throw new IllegalStateException("Trezor is not valid");
      }

      trezor.sendMessage(trezorMessage);

      // Block until response arrives for the specified duration
      event = getTrezorEventQueue().poll(duration, timeUnit);
      if (event != null) {
        handleTrezorEvent(event);
      } else {
        // Timeout so unexpected EOF
        return TrezorEvents.newSystemEvent(TrezorEventType.DEVICE_EOF);
      }

      if (!isTrezorValid) {
        throw new IllegalStateException("Trezor is not valid");
      }

      return event;

    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }

  }

  private void handleTrezorEvent(TrezorEvent event) {

    // Decode into a message type for use with a switch
    Optional<MessageType> messageType = event.protocolMessageType();
    if (event.protocolMessage().isPresent()) {

      // Protocol message

      log.debug("Received event: {}", event.protocolMessage().get().getClass().getName());
      log.debug("{}", event.protocolMessage().get().toString());
      log.debug("Message type: {}", messageType.get().name());

      if (MessageType.FAILURE.equals(messageType.get())) {
        log.error("Failure: {}", ((TrezorMessage.Failure) event.protocolMessage().get()).getMessage());
      }

      if (MessageType.BUTTON_REQUEST.equals(messageType.get())) {
        sendBlockingMessage(TrezorMessage.ButtonAck.getDefaultInstance(), 10, TimeUnit.SECONDS);
      }

    } else {
      if (
        TrezorEventType.DEVICE_DISCONNECTED.equals(event.eventType()) ||
          TrezorEventType.DEVICE_FAILURE.equals(event.eventType())) {

        // Stop further processing
        close();

      }
    }
  }

  /**
   * @param size The number of bytes of random data required
   *
   * @return The random bytes (based on SecureRandom implementation)
   */
  public byte[] newEntropy(int size) {
    byte[] entropy = new byte[size];
    secureRandom.nextBytes(entropy);
    return entropy;
  }

  @Override
  public BlockingQueue<TrezorEvent> getTrezorEventQueue() {
    return trezorEventQueue;
  }

  @Override
  public void setTrezorEventQueue(BlockingQueue<TrezorEvent> trezorEventQueue) {
    this.trezorEventQueue = trezorEventQueue;
  }
}
