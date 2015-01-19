package org.multibit.hd.hardware.core.fsm;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.wallet.KeyChain;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletEventType;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.events.MessageEvents;
import org.multibit.hd.hardware.core.messages.Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * <p>State context to provide the following to hardware wallet finite state machine:</p>
 * <ul>
 * <li>Provision of state context and parameters</li>
 * </ul>
 *
 * <p>The finite state machine (FSM) for the hardware wallet requires and tracks various
 * input parameters from the user and from the external environment. This context provides
 * a single location to store these values during state transitions.</p>
 *
 * @since 0.0.1
 *  
 */
public class HardwareWalletContext {

  private static final Logger log = LoggerFactory.getLogger(HardwareWalletContext.class);

  /**
   * The hardware wallet client handling outgoing messages and generating low level
   * message events
   */
  private final HardwareWalletClient client;

  /**
   * The current state should start by assuming an attached device and progress from there
   * to either detached or connected
   */
  private HardwareWalletState currentState = HardwareWalletStates.newAttachedState();

  /**
   * We begin at the start
   */
  private ContextUseCase currentUseCase = ContextUseCase.START;

  /**
   * Provide contextual information for the current wallet creation use case
   */
  private Optional<CreateWalletSpecification> createWalletSpecification = Optional.absent();

  /**
   * Provide contextual information for the current wallet load use case
   */
  private Optional<LoadWalletSpecification> loadWalletSpecification = Optional.absent();

  /**
   * Provide the features
   */
  private Optional<Features> features = Optional.absent();

  /**
   * Provide the transaction forming the basis for the "sign transaction" use case
   */
  private Optional<Transaction> transaction = Optional.absent();
  /**
   * Keep track of all the signatures for the "sign transaction" use case
   */
  private Map<Integer, byte[]> signatures = Maps.newHashMap();

  /**
   * Keep track of any transaction serialization bytes coming back from the device
   */
  private ByteArrayOutputStream serializedTx = new ByteArrayOutputStream();

  /**
   * A map keyed on TxInput index and the associated path to the receiving address on that input
   * This is used during Trezor transaction signing for fast addressN lookup
   */
  private Map<Integer, ImmutableList<ChildNumber>> receivingAddressPathMap;

  /**
   * A map keyed on TxOutput Address and the associated path to the change address on that output
   * This is used during Trezor transaction signing for fast addressN lookup, removes the need
   * to confirm a change address and ensures the transaction balance is correct on the display
   */
  private Map<Address, ImmutableList<ChildNumber>> changeAddressPathMap;

  /**
   * Provide a list of child numbers defining the HD path required.
   * In the case of an extended public key this will result in multiple calls to retrieve each parent
   * due to hardening.
   *
   * The end result can be found in {@link #deterministicHierarchy}.
   */
  private Optional<List<ChildNumber>> childNumbers = Optional.absent();

  /**
   * Provide a deterministic key representing the root node of the current HD path.
   * As the child numbers are explored one by one this deterministic key maintains the chain
   * of keys derived from the base 58 xpub.
   *
   * The end result can be found in {@link #deterministicHierarchy}.
   */
  private Optional<DeterministicKey> deterministicKey = Optional.absent();
  /**
   * Provide a deterministic hierarchy containing all hardened extended public keys so that a
   * watching wallet can be created
   */
  private Optional<DeterministicHierarchy> deterministicHierarchy = Optional.absent();

  /**
   * Entropy returned from the Trezor (result of encryption of fixed text)
   */
  private Optional<byte[]> entropy = Optional.absent();

  /**
   * @param client The hardware wallet client
   */
  public HardwareWalletContext(HardwareWalletClient client) {

    this.client = client;

    // Ensure the service is subscribed to low level message events from the client
    MessageEvents.subscribe(this);

    // Verify the environment
    if (!client.attach()) {
      log.warn("Cannot start the service due to a failed environment.");
      throw new IllegalStateException("Cannot start the service due to a failed environment.");
    }

  }

  /**
   * @return The wallet features (e.g. PIN required, label etc)
   */
  public Optional<Features> getFeatures() {
    return features;
  }

  public void setFeatures(Features features) {
    this.features = Optional.fromNullable(features);
  }

  /**
   * @return The transaction for the "sign transaction" use case
   */
  public Optional<Transaction> getTransaction() {
    return transaction;
  }

  /**
   * @return The current hardware wallet state
   */
  public HardwareWalletState getState() {
    return currentState;
  }

  /**
   * <p>Note: When adding this signature using the builder setScriptSig() you'll need to append the SIGHASH 0x01 byte</p>
   *
   * @return The map of ECDSA signatures provided by the device during "sign transaction" keyed on the input index
   */
  public Map<Integer, byte[]> getSignatures() {
    return signatures;
  }

  /**
   * This value cannot be considered complete until the "SHOW_OPERATION_SUCCESSFUL" message has been received
   * since it can be built up over several incoming messages
   *
   * @return The serialized transaction provided by the device during "sign transaction"
   */
  public ByteArrayOutputStream getSerializedTx() {
    return serializedTx;
  }

  /**
   * <p>Reset all context state to ensure a fresh context</p>
   */
  private void resetAll() {

    createWalletSpecification = Optional.absent();
    loadWalletSpecification = Optional.absent();

    features = Optional.absent();

    transaction = Optional.absent();
    signatures = Maps.newHashMap();

    serializedTx = new ByteArrayOutputStream();
    receivingAddressPathMap = Maps.newHashMap();
    changeAddressPathMap = Maps.newHashMap();

    childNumbers = Optional.absent();
    deterministicKey = Optional.absent();
    deterministicHierarchy = Optional.absent();

    entropy = Optional.absent();

  }

  /**
   * <p>Reset all context state, excepting Features, to ensure a fresh context</p>
   */
  private void resetAllButFeatures() {

    Optional<Features> originalFeatures = this.features;

    resetAll();

    this.features = originalFeatures;

  }

  /**
   * <p>Reset the context into a stopped state (the service will have to be stopped and a new one started)</p>
   */
  public void resetToStopped() {

    log.debug("Reset to 'stopped'");

    // Clear relevant information
    resetAllButFeatures();

    // Issue a hard detach - we are done
    client.hardDetach();

    // Unsubscribe from events
    MessageEvents.unsubscribe(this);

    // Perform the state change
    currentState = HardwareWalletStates.newStoppedState();

    // Fire the high level event
    HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_DEVICE_STOPPED);

  }

  /**
   * <p>Reset the context back to a failed state (retain device information but prevent further communication)</p>
   */
  public void resetToFailed() {

    log.debug("Reset to 'failed'");

    // Clear relevant information
    resetAllButFeatures();

    // Perform the state change
    currentState = HardwareWalletStates.newFailedState();

    // Fire the high level event
    HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_DEVICE_FAILED);
  }

  /**
   * <p>Reset the context back to a detached state (no device information)</p>
   */
  public void resetToDetached() {

    log.debug("Reset to 'detached'");

    // Clear relevant information
    resetAll();

    // Perform the state change
    currentState = HardwareWalletStates.newDetachedState();

    // Fire the high level event
    HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_DEVICE_DETACHED);
  }

  /**
   * <p>Reset the context back to an attached state (clear device information and transition to connected)</p>
   */
  public void resetToAttached() {

    log.debug("Reset to 'attached'");

    // Clear relevant information
    resetAll();

    // Perform the state change
    currentState = HardwareWalletStates.newAttachedState();

    // No high level event for this state
  }

  /**
   * <p>Reset the context back to a connected state (clear device information and transition to initialised)</p>
   */
  public void resetToConnected() {

    log.debug("Reset to 'connected'");

    // Clear relevant information
    resetAll();

    // Perform the state change
    currentState = HardwareWalletStates.newConnectedState();

    // No high level event for this state
  }

  /**
   * <p>Reset the context back to a disconnected state (device is attached but communication has not been established)</p>
   */
  public void resetToDisconnected() {

    log.debug("Reset to 'disconnected'");

    // Clear relevant information
    resetAll();

    // Perform the state change
    currentState = HardwareWalletStates.newDisconnectedState();

    // No high level event for this state

  }

  /**
   * <p>Reset the context back to the initialised state (standard awaiting state with features)</p>
   */
  public void resetToInitialised() {

    log.debug("Reset to 'initialised'");

    // Clear relevant information
    resetAllButFeatures();

    // Perform the state change
    currentState = HardwareWalletStates.newInitialisedState();

    // Fire the high level event
    HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_DEVICE_READY, features.get());
  }

  /**
   * @return The hardware wallet client
   */
  public HardwareWalletClient getClient() {
    return client;
  }

  /**
   * @return The create wallet specification
   */
  public Optional<CreateWalletSpecification> getCreateWalletSpecification() {
    return createWalletSpecification;
  }

  /**
   * @return The load wallet specification
   */
  public Optional<LoadWalletSpecification> getLoadWalletSpecification() {
    return loadWalletSpecification;
  }

  /**
   * @return The map of paths for our receiving addresses on the current transaction (key input index, value deterministic path to receiving address)
   */
  public Map<Integer, ImmutableList<ChildNumber>> getReceivingAddressPathMap() {
    return receivingAddressPathMap;
  }

  /**
   * @return The map of paths for the change address (key address, value deterministic path to change address)
   */
  public Map<Address, ImmutableList<ChildNumber>> getChangeAddressPathMap() {
    return changeAddressPathMap;
  }

  /**
   * @return The list of child numbers representing a HD path for the current use case
   */
  public Optional<List<ChildNumber>> getChildNumbers() {
    return childNumbers;
  }

  /**
   * @return The deterministic key derived from the xpub of the given child numbers for the current use case
   */
  public Optional<DeterministicKey> getDeterministicKey() {
    return deterministicKey;
  }

  public void setDeterministicKey(DeterministicKey deterministicKey) {
    log.debug("Setting deterministic key"); // Avoid logging the deterministic key
    this.deterministicKey = Optional.fromNullable(deterministicKey);
  }

  /**
   * @return The deterministic hierarchy based on the deterministic key for the current use case
   */
  public Optional<DeterministicHierarchy> getDeterministicHierarchy() {
    return deterministicHierarchy;
  }

  public void setDeterministicHierarchy(DeterministicHierarchy deterministicHierarchy) {
    this.deterministicHierarchy = Optional.fromNullable(deterministicHierarchy);
  }

  /**
   * @return The current use case
   */
  public ContextUseCase getCurrentUseCase() {
    return currentUseCase;
  }

  /**
   * @param event The low level message event
   */
  @Subscribe
  public void onMessageEvent(MessageEvent event) {

    log.debug("Received message event: '{}'", event.getEventType().name());

    // Perform a state transition as a result of this event
    try {
      currentState.transition(client, this, event);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Sets to "confirm reset" state
   */
  public void setToConfirmResetState() {

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmResetState();

    // Expect the specification to be in place
    CreateWalletSpecification specification = createWalletSpecification.get();

    // Issue starting message to elicit the event
    client.resetDevice(
      specification.getLanguage(),
      specification.getLabel(),
      specification.isDisplayRandom(),
      specification.isPinProtection(),
      specification.getStrength()
    );

  }

  /**
   * Sets to "confirm load" state
   */
  public void setToConfirmLoadState() {

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmLoadState();

    // Expect the specification to be in place
    LoadWalletSpecification specification = loadWalletSpecification.get();

    // Issue starting message to elicit the event
    client.loadDevice(
      specification.getLanguage(),
      specification.getLabel(),
      specification.getSeedPhrase(),
      specification.getPin()
    );

  }

  /**
   * <p>Change or remove the device PIN.</p>
   *
   * @param remove True if an existing PIN should be removed
   */
  public void beginChangePIN(boolean remove) {

    log.debug("Begin 'change PIN' use case");

    // Clear relevant information
    resetAllButFeatures();

    // Track the use case
    currentUseCase = ContextUseCase.CHANGE_PIN;

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmChangePINState();

    // Issue starting message to elicit the event
    client.changePIN(remove);
  }

  /**
   * <p>Continue the "change PIN" use case with the provision of the current PIN</p>
   *
   * @param pin The PIN
   */
  public void continueChangePIN_PIN(String pin) {

    log.debug("Continue 'change PIN' use case (provide PIN)");

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmChangePINState();

    // Issue starting message to elicit the event
    client.pinMatrixAck(pin);

  }

  /**
   * <p>Begin the "wipe device" use case</p>
   */
  public void beginWipeDeviceUseCase() {

    log.debug("Begin 'wipe device' use case");

    // Clear relevant information
    resetAllButFeatures();

    // Track the use case
    currentUseCase = ContextUseCase.WIPE_DEVICE;

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmWipeState();

    // Issue starting message to elicit the event
    client.wipeDevice();
  }

  /**
   * <p>Begin the "get address" use case</p>
   *
   * @param account     The plain account number (0 gives maximum compatibility)
   * @param keyPurpose  The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index       The plain index of the required address
   * @param showDisplay True if the device should display the same address to allow the user to verify no tampering has occurred (recommended).
   */
  public void beginGetAddressUseCase(int account, KeyChain.KeyPurpose keyPurpose, int index, boolean showDisplay) {

    log.debug("Begin 'get address' use case");

    // Clear relevant information
    resetAllButFeatures();

    // Track the use case
    currentUseCase = ContextUseCase.REQUEST_ADDRESS;

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmGetAddressState();

    // Issue starting message to elicit the event
    client.getAddress(
      account,
      keyPurpose,
      index,
      showDisplay
    );

  }

  /**
   * <p>Begin the "get public key" use case</p>
   *
   * @param account    The plain account number (0 gives maximum compatibility)
   * @param keyPurpose The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index      The plain index of the required address
   */
  public void beginGetPublicKeyUseCase(int account, KeyChain.KeyPurpose keyPurpose, int index) {

    log.debug("Begin 'get public key' use case");

    // Clear relevant information
    resetAllButFeatures();

    // Track the use case
    currentUseCase = ContextUseCase.REQUEST_PUBLIC_KEY;

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmGetPublicKeyState();

    // Issue starting message to elicit the event
    client.getPublicKey(
      account,
      keyPurpose,
      index
    );

  }

  /**
   * <p>Begin the "get deterministic hierarchy" use case</p>
   *
   * @param childNumbers The child numbers describing the required chain from the master node including hardening bits
   */
  public void beginGetDeterministicHierarchyUseCase(List<ChildNumber> childNumbers) {

    log.debug("Begin 'get deterministic hierarchy' use case");

    // Clear relevant information
    resetAllButFeatures();

    // Track the use case
    currentUseCase = ContextUseCase.REQUEST_DETERMINISTIC_HIERARCHY;

    // Store the overall context parameters
    this.childNumbers = Optional.of(childNumbers);

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmGetDeterministicHierarchyState();

    // Issue starting message to elicit the event
    // In this case we start with the master node (empty list) to enable building up a complete
    // hierarchy in case of hardened child numbers that require private keys to create
    client.getDeterministicHierarchy(Lists.<ChildNumber>newArrayList());

  }

  /**
   * <p>Begin the "sign message key use case</p>
   *
   * @param account    The plain account number (0 gives maximum compatibility)
   * @param keyPurpose The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index      The plain index of the required address
   * @param message    The message for signing
   */
  public void beginSignMessageUseCase(
    int account,
    KeyChain.KeyPurpose keyPurpose,
    int index,
    byte[] message
  ) {

    log.debug("Begin 'sign message' use case");

    // Clear relevant information
    resetAllButFeatures();

    // Track the use case
    currentUseCase = ContextUseCase.SIGN_MESSAGE;

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmSignMessageState();

    // Issue starting message to elicit the event
    client.signMessage(
      account,
      keyPurpose,
      index,
      message
    );
  }

  /**
   * <p>Continue the "sign message" use case with the provision of the current PIN</p>
   *
   * @param pin The PIN
   */
  public void continueSignMessage_PIN(String pin) {

    log.debug("Continue 'sign message' use case (provide PIN)");

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmSignMessageState();

    // Issue starting message to elicit the event
    client.pinMatrixAck(pin);

  }

  /**
   * <p>Begin the "cipher key" use case</p>
   *
   * @param account      The plain account number (0 gives maximum compatibility)
   * @param keyPurpose   The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index        The plain index of the required address
   * @param key          The cipher key (e.g. "Some text")
   * @param keyValue     The key value (e.g. "[16 bytes of random data]")
   * @param isEncrypting True if encrypting
   * @param askOnDecrypt True if device should ask on decrypting
   * @param askOnEncrypt True if device should ask on encrypting
   */
  public void beginCipherKeyUseCase(
    int account,
    KeyChain.KeyPurpose keyPurpose,
    int index,
    byte[] key,
    byte[] keyValue,
    boolean isEncrypting,
    boolean askOnDecrypt,
    boolean askOnEncrypt
  ) {

    log.debug("Begin 'cipher key' use case");

    // Track the use case
    currentUseCase = ContextUseCase.REQUEST_CIPHER_KEY;

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmCipherKeyState();

    // Issue starting message to elicit the event
    client.cipherKeyValue(
      account,
      keyPurpose,
      index,
      key,
      keyValue,
      isEncrypting,
      askOnDecrypt,
      askOnEncrypt
    );

  }

  /**
   * <p>Continue the "cipher key" use case with the provision of the current PIN</p>
   *
   * @param pin The PIN
   */
  public void continueCipherKey_PIN(String pin) {

    log.debug("Continue 'cipher key' use case (provide PIN)");

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmCipherKeyState();

    // Issue starting message to elicit the event
    client.pinMatrixAck(pin);

  }

  /**
   * <p>Begin the "load wallet" use case</p>
   *
   * @param specification The specification describing the use of PIN, seed strength etc
   */
  public void beginLoadWallet(LoadWalletSpecification specification) {

    log.debug("Begin 'load wallet' use case");

    // Clear relevant information
    resetAllButFeatures();

    // Track the use case
    currentUseCase = ContextUseCase.LOAD_WALLET;

    // Store the overall context parameters
    this.loadWalletSpecification = Optional.fromNullable(specification);

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmWipeState();

    // Issue starting message to elicit the event
    client.wipeDevice();

  }

  /**
   * <p>Continue the "load wallet" use case with the provision of a PIN (either first or second)</p>
   *
   * @param pin The PIN
   */
  public void continueLoadWallet_PIN(String pin) {

    log.debug("Continue 'create wallet on device' use case (provide PIN)");

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmPINState();

    // Issue starting message to elicit the event
    client.pinMatrixAck(pin);

  }

  /**
   * <p>Begin the "create wallet on device" use case</p>
   *
   * @param specification The specification describing the use of PIN, seed strength etc
   */
  public void beginCreateWallet(CreateWalletSpecification specification) {

    log.debug("Begin 'create wallet on device' use case");

    // Clear relevant information
    resetAllButFeatures();

    // Track the use case
    currentUseCase = ContextUseCase.CREATE_WALLET;

    // Store the overall context parameters
    this.createWalletSpecification = Optional.fromNullable(specification);

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmWipeState();

    // Issue starting message to elicit the event
    client.wipeDevice();

  }

  /**
   * <p>Continue the "create wallet on device" use case with the provision of a PIN (either first or second)</p>
   *
   * @param pin The PIN
   */
  public void continueCreateWallet_PIN(String pin) {

    log.debug("Continue 'create wallet on device' use case (provide PIN)");

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmPINState();

    // Issue starting message to elicit the event
    client.pinMatrixAck(pin);

  }

  /**
   * <p>Continue the "create wallet on device" use case with the provision of entropy</p>
   *
   * @param entropy The 256 bits of entropy to include
   */
  public void continueCreateWallet_Entropy(byte[] entropy) {

    log.debug("Continue 'create wallet on device' use case (provide entropy)");

    // Track the use case
    currentUseCase = ContextUseCase.PROVIDE_ENTROPY;

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmEntropyState();

    // Issue starting message to elicit the event
    client.entropyAck(entropy);

  }

  /**
   * <p>Begin the "sign transaction" use case</p>
   *
   * @param transaction             The transaction containing the inputs and outputs
   * @param receivingAddressPathMap The map of paths for our receiving addresses
   * @param changeAddressPathMap    The map paths for our change address
   */
  public void beginSignTxUseCase(
    Transaction transaction,
    Map<Integer, ImmutableList<ChildNumber>> receivingAddressPathMap,
    Map<Address, ImmutableList<ChildNumber>> changeAddressPathMap) {

    log.debug("Begin 'sign transaction' use case");

    // Clear relevant information
    resetAllButFeatures();

    // Track the use case
    currentUseCase = ContextUseCase.SIGN_TX;

    // Store the overall context parameters
    this.transaction = Optional.of(transaction);
    this.receivingAddressPathMap = receivingAddressPathMap;
    this.changeAddressPathMap = changeAddressPathMap;

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmSignTxState();

    // Issue starting message to elicit the event
    client.signTx(transaction);

  }

  /**
   * <p>Continue the "sign transaction" use case with the provision of the current PIN</p>
   *
   * @param pin The PIN
   */
  public void continueSignTx_PIN(String pin) {

    log.debug("Continue 'sign tx' use case (provide PIN)");

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmSignTxState();

    // Issue starting message to elicit the event
    client.pinMatrixAck(pin);

  }

  /**
   * @return Entropy returned from the Trezor (result of encryption of fixed text)
   */
  public Optional<byte[]> getEntropy() {
    return entropy;
  }

  /**
   * <p>This is normally called after a successful cipher key operation</p>
   *
   * @param entropy The entropy to set
   */
  public void setEntropy(byte[] entropy) {
    this.entropy = Optional.fromNullable(entropy);
  }

}
