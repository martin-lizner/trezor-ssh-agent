package org.multibit.hd.hardware.core.fsm;

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
   * <p>The current state should start assuming an attached device and progress from there
   * to either detached or connected</p>
   */
  private HardwareWalletState currentState = HardwareWalletStates.newAttachedState();

  private boolean creatingWallet;

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

    // Perform the state change
    currentState = HardwareWalletStates.newDetachedState();

    // Fire the high level event
    HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_DEVICE_DETACHED);
  }


  /**
   * <p>Reset the context back to an attached state (retain device information but prevent further communication)</p>
   */
  public void resetToAttached() {

    log.debug("Reset to 'attached'");

    // Perform the state change
    currentState = HardwareWalletStates.newAttachedState();

    // No high level event for this state
  }

  /**
   * <p>Reset the context back to a connected state (retain device information and allow further communication)</p>
   */
  public void resetToConnected() {

    log.debug("Reset to 'connected'");

    // Perform the state change
    currentState = HardwareWalletStates.newConnectedState();

    // No high level event for this state
  }

  /**
   * <p>Reset the context back to a disconnected state (device is attached but communication has not been established)</p>
   */
  public void resetToDisconnected() {

    log.debug("Reset to 'disconnected'");

    // Perform the state change
    currentState = HardwareWalletStates.newDisconnectedState();

    // No high level event for this state

  }

  /**
   * <p>Reset the context back to the initialised state (standard awaiting state)</p>
   */
  public void resetToInitialised() {

    log.debug("Reset to 'initialised'");

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
   * <p>Begin the "reset device" use case</p>
   *
   * @param language             The language code (e.g. "en")
   * @param label                The label to display below the logo (e.g "Fred")
   * @param displayRandom        True if the device should display the entropy generated by the device before asking for additional entropy
   * @param passphraseProtection True if the master node is protected with a pass phrase
   * @param pinProtection        True if the device should use PIN protection
   * @param wordCount            The number of words in the seed phrase (12 (default) is 128 bits, 18 is 196 bits, 24 is 256 bits)
   *
   *                             TODO Perform extract parameter object refactoring
   */
  public void beginResetDeviceUseCase(
    String language,
    String label,
    boolean displayRandom,
    boolean passphraseProtection,
    boolean pinProtection,
    int wordCount
  ) {

    log.debug("Begin 'reset device' use case");

    // Store the overall context parameters

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

  public boolean isCreatingWallet() {
    return creatingWallet;
  }


  public void setCreatingWallet(boolean creatingWallet) {
    this.creatingWallet = creatingWallet;
  }

  /**
   * @param event The low level message event
   */
  @Subscribe
  public void onMessageEvent(MessageEvent event) {

    currentState.transition(client, this, event);

  }

}
