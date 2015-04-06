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
 * <p>The "confirm wipe" state occurs in response to a WIPE message and handles button
 * requests, success and failure messages coming from the device.</p>
 *
 * @since 0.0.1
 *  
 */
public class ConfirmWipeState extends AbstractHardwareWalletState {

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      case BUTTON_REQUEST:
        // Device is asking for confirmation to wipe
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_BUTTON_PRESS, event.getMessage().get());
        client.buttonAck();
        break;
      case SUCCESS:
        // Device has successfully wiped
        switch (context.getCurrentUseCase()) {
          case CREATE_WALLET:
            // Proceed to wallet reset (internal seed)
            context.setToConfirmResetState();
            break;
          case LOAD_WALLET:
            // Proceed to wallet load (provided seed)
            context.setToConfirmLoadState();
            break;
          default:
            // No wallet creation required so we're done
            HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_SUCCEEDED, event.getMessage().get());
            // Ensure the Features are updated
            context.resetToConnected();
            break;
        }
        break;
      case FAILURE:
        // User has cancelled or operation failed
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_FAILED, event.getMessage().get());
        context.resetToInitialised();
        break;
      default:
        handleUnexpectedMessageEvent(context, event);
    }

  }
}
