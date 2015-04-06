package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.MessageEvent;

/**
 * <p>State to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>State transitions based on low level message events</li>
 * </ul>
 * <p>The "disconnected" state is the starting state of the hardware wallet service when it is first started.</p>
 *
 * @since 0.0.1
 *  
 */
public class DisconnectedState extends AbstractHardwareWalletState {

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      // TODO Implement
      default:
        handleUnexpectedMessageEvent(context, event);
    }
  }
}
