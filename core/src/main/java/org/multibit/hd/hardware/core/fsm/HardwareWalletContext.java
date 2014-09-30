package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.messages.Features;

/**
 * <p>State context to provide the following to hardware wallet finite state machine:</p>
 * <ul>
 * <li>Provision of state context and parameters</li>
 * </ul>
 *
 * <p>The finite state machine (FSM) for the hardware wallet requires various input parameters
 * from the user and from the external environment. This context provides a single location to
 * store these values during state transitions.</p>
 *
 * @since 0.0.1
 *  
 */
public class HardwareWalletContext {

  private Features features;
  private HardwareWalletState currentState = HardwareWalletStates.newDetachedState();

  public HardwareWalletContext() {

  }

  /**
   * @return The wallet features (e.g. PIN required, label etc)
   */
  public Features getFeatures() {
    return features;
  }

  public void setFeatures(Features features) {
    this.features = features;
  }

  /**
   * @return The current hardware wallet state
   */
  public HardwareWalletState getState() {
    return currentState;
  }

  /**
   * Entry point to the overall state machine
   */
  public void verifyEnvironment() {
  }

  /**
   * Reset the context back to a failed state (retain device information but prevent further communication)
   */
  public void resetToFailed() {

    // Perform the state change
    currentState = HardwareWalletStates.newFailedState();
  }

  /**
   * Reset the context back to a detached state (no device information)
   */
  public void resetToDetached() {

    // Perform the state change
    currentState = HardwareWalletStates.newDetachedState();
  }

  /**
   * Reset the context back to an attached state (retain device information but prevent further communication)
   */
  public void resetToAttached() {

    // Perform the state change
    currentState = HardwareWalletStates.newAttachedState();
  }


  /**
   * Reset the context back to a connected state (retain device information and allow further communication)
   */
  public void resetToConnected() {

    // Perform the state change
    currentState = HardwareWalletStates.newConnectedState();
  }

  /**
   * Reset the context back to a disconnected state (device is attached but communication has not been established)
   */
  public void resetToDisconnected() {

    // Perform the state change
    currentState = HardwareWalletStates.newDisconnectedState();
  }
}
