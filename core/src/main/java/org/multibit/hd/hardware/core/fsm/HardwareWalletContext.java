package org.multibit.hd.hardware.core.fsm;

import com.google.bitcoin.core.Transaction;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEventType;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.messages.Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private Optional<Features> features = Optional.absent();

  /**
   * The hardware wallet client handling outgoing messages and generating low level
   * message events
   */
  private final HardwareWalletClient client;

  /**
   * The current state should start assuming an attached device and progress from there
   * to either detached or connected
   */
  private HardwareWalletState currentState = HardwareWalletStates.newAttachedState();

  /**
   * Provide contextual information for the current wallet creation use case
   */
  private Optional<CreateWalletSpecification> createWalletSpecification = Optional.absent();

  /**
   * Provide the transaction forming the basis for the "sign transaction" use case
   */
  private Optional<Transaction> transaction = Optional.absent();

  /**
   * @param client The hardware wallet client
   */
  public HardwareWalletContext(HardwareWalletClient client) {

    this.client = client;

    // Ensure the service is subscribed to low level message events
    // from the client
    HardwareWalletService.messageEventBus.register(this);

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
   * <p>Reset the context back to a failed state (retain device information but prevent further communication)</p>
   */
  public void resetToFailed() {

    log.debug("Reset to 'failed'");

    // Clear relevant information
    createWalletSpecification = Optional.absent();

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
    createWalletSpecification = Optional.absent();
    features = Optional.absent();

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
    createWalletSpecification = Optional.absent();
    features = Optional.absent();

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
    createWalletSpecification = Optional.absent();
    features = Optional.absent();

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
    createWalletSpecification = Optional.absent();

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
    createWalletSpecification = Optional.absent();

    // Perform the state change
    currentState = HardwareWalletStates.newInitialisedState();

    // Fire the high level event
    HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_DEVICE_READY);
  }

  /**
   * <p>Begin the "wipe device" use case</p>
   */
  public void beginWipeDeviceUseCase() {

    log.debug("Begin 'wipe device' use case");

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmWipeState();

    // Issue starting message to elicit the event
    client.wipeDevice();
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
   * @param event The low level message event
   */
  @Subscribe
  public void onMessageEvent(MessageEvent event) {

    log.debug("Received message event: '{}'.'{}'", event.getEventType().name(), event.getMessage());

    // Perform a state transition as a result of this event
    currentState.transition(client, this, event);

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
   * <p>Begin the "get address" use case</p>
   *
   * @param index       The index of the chain node from the master node
   * @param value       The index of the address from the given chain node
   * @param showDisplay True if the device should display the same address to allow the user to verify no tampering has occurred.
   */
  public void beginGetAddressUseCase(int index, int value, boolean showDisplay) {

    log.debug("Begin 'get address' use case");

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmGetAddressState();

    // Issue starting message to elicit the event
    client.getAddress(
      index,
      value,
      showDisplay
    );

  }

  /**
   * <p>Begin the "create wallet on device" use case</p>
   *
   * @param createWalletSpecification The specification describing the use of PIN, passphrase, seed strength etc
   */
  public void beginCreateWallet(CreateWalletSpecification createWalletSpecification) {

    log.debug("Begin 'create wallet on device' use case");

    // Store the overall context parameters
    this.createWalletSpecification = Optional.fromNullable(createWalletSpecification);

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

    // Store the overall context parameters

    // Set the event receiving state
    currentState = HardwareWalletStates.newConfirmEntropyState();

    // Issue starting message to elicit the event
    client.entropyAck(entropy);

  }

  /**
   * <p>Begin the "sign transaction" use case</p>
   *
   * @param transaction The transaction containing the inputs and outputs
   */
  public void beginSignTxUseCase(Transaction transaction) {

    log.debug("Begin 'sign transaction' use case");

    // Store the overall context parameters
    this.transaction = Optional.of(transaction);

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

}
