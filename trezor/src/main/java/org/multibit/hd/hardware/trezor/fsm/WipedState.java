package org.multibit.hd.hardware.trezor.fsm;

import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.fsm.AbstractHardwareWalletState;

/**
 * <p>State to provide the following to Trezor clients:</p>
 * <ul>
 * <li>Event-based state transitions</li>
 * </ul>
 * <p>The "wiped" state is the starting state of a Trezor when it is removed from the box and plugged in.</p>
 * @since 0.0.1
 *  
 */
public class WipedState extends AbstractHardwareWalletState {

  @Override
  public void handleEvent(HardwareWalletEvent hardwareWalletEvent) {

    switch (hardwareWalletEvent.getMessageType()) {
      case ENTROPY_REQUEST:

        break;
      default:

    }

  }

}
