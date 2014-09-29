package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.events.HardwareWalletEvent;

/**
 * <p>State to provide the following to hardware wallet finite state machines:</p>
 * <ul>
 * <li>Standard methods for transitions</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public interface HardwareWalletState {
  /**
   * @param hardwareWalletEvent An event containing the protobuf message
   */
  void handleEvent(HardwareWalletEvent hardwareWalletEvent);

  /**
   * @return True if this state is currently active
   */
  boolean isActive();

  /**
   * @param isActive True if this state should be active
   */
  void setActive(boolean isActive);
}
