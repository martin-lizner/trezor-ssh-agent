package org.multibit.hd.hardware.core.fsm;

import com.google.common.base.Optional;
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
   * The current state should start assuming an attached device and progress from there
   * to either detached or connected
   */
  private HardwareWalletState currentState = HardwareWalletStates.newAttachedState();

  public HardwareWalletContext() {

  }

  /**
   * Entry point to the state machine
   */
  public void awaitConnection() {

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
   * Reset the context back to a failed state (retain device information but prevent further communication)
   */
  public void resetToFailed() {

    log.debug("Reset to 'failed'");

    // Perform the state change
    currentState = HardwareWalletStates.newFailedState();
  }

  /**
   * Reset the context back to a detached state (no device information)
   */
  public void resetToDetached() {

    log.debug("Reset to 'detached'");

    // Perform the state change
    currentState = HardwareWalletStates.newDetachedState();
  }


  /**
   * Reset the context back to an attached state (retain device information but prevent further communication)
   */
  public void resetToAttached() {

    log.debug("Reset to 'attached'");

    // Perform the state change
    currentState = HardwareWalletStates.newAttachedState();
  }

  /**
   * Reset the context back to a connected state (retain device information and allow further communication)
   */
  public void resetToConnected() {

    log.debug("Reset to 'connected'");

    // Perform the state change
    currentState = HardwareWalletStates.newConnectedState();
  }

  /**
   * Reset the context back to a disconnected state (device is attached but communication has not been established)
   */
  public void resetToDisconnected() {

    log.debug("Reset to 'disconnected'");

    // Perform the state change
    currentState = HardwareWalletStates.newDisconnectedState();
  }

  /**
   * Reset the context back to the initialised state (standard awaiting state)
   */
  public void resetToInitialised() {

    log.debug("Reset to 'initialised'");

    // Perform the state change
    currentState = HardwareWalletStates.newInitialisedState();
  }

}
