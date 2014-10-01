package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletEventType;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger log = LoggerFactory.getLogger(ConfirmWipeState.class);

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      case BUTTON_REQUEST:
        // Device is asking for confirmation to wipe
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_BUTTON_PRESS);
        client.buttonAck();
        break;
      case SUCCESS:
        // Device has successfully wiped
        if (context.isCreatingWallet()) {
          // Use the context to provide the parameters for the next state

        } else {
          // No wallet creation required so we're done
          HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_SUCCEEDED, event.getMessage().get());
          context.resetToInitialised();
        }
        break;
      case FAILURE:
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_FAILED, event.getMessage().get());
        context.resetToInitialised();
        break;
      default:
        log.warn("Unexpected message event '{}'", event.getEventType().name());
        context.resetToConnected();
    }

  }
}
