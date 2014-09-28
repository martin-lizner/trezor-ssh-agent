package org.multibit.hd.hardware.core;

import org.multibit.hd.hardware.core.events.HardwareWalletEvent;

/**
 * <p>State to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>Standard entry points for events and transitions</li>
 * </ul>
 *
 * <p></p>
 *
 * @since 0.0.1
 *  
 */
public abstract class AbstractHardwareWalletState {

  /**
   * True if this state can respond to events
   */
  private boolean isActive = false;

  public AbstractHardwareWalletState() {

    // Ensure we are subscribed to hardware wallet events
    HardwareWalletService.hardwareEventBus.register(this);

  }

  /**
   * <p>Provide standard handling of system events before handing over to implementations</p>
   *
   * @param hardwareWalletEvent The hardware wallet event
   */
  public void onHardwareWalletEvent(HardwareWalletEvent hardwareWalletEvent) {

    // Ignore events if not active
    if (!isActive) {
      return;
    }

    // TODO Handle system events

    // Hand over to the dedicated event handler
    handleEvent(hardwareWalletEvent);

  }

  public abstract void handleEvent(HardwareWalletEvent hardwareWalletEvent);
}
