package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletEventType;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvent;

/**
 * <p>State to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>State transitions based on low level message events</li>
 * </ul>
 * <p>The "initialised" state is the awaiting state of a hardware wallet when the
 * features have been retrieved.</p>
 *
 * <p>The next state depends upon a user providing input based on the features.</p>
 *
 * @since 0.0.1
 *  
 */
public class InitialisedState extends AbstractHardwareWalletState {

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    // We don't expect any messages
    switch (event.getEventType()) {
      case SUCCESS:
        // Possible Ping
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_SUCCEEDED, event.getMessage().get(), client.name());
        // Ensure the Features are updated
        context.resetToConnected();
        break;
      default:
        handleUnexpectedMessageEvent(context, event);
    }

  }

}
