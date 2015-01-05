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
 * <p>The "confirm entropy" state occurs in response to a ENTROPY_REQUEST message and handles
 * the ongoing button requests, success and failure messages coming from the device as it
 * shows the words in the generated seed phrase.</p>
 *
 * @since 0.0.1
 *  
 */
public class ConfirmEntropyState extends AbstractHardwareWalletState {

  private static final Logger log = LoggerFactory.getLogger(ConfirmEntropyState.class);

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      case BUTTON_REQUEST:
        // Device is asking for the user to acknowledge a word display
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_BUTTON_PRESS, event.getMessage().get());
        client.buttonAck();
        break;
      case SUCCESS:
        // Device has completed the create wallet operation
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_SUCCEEDED, event.getMessage().get());
        // Ensure the Features are updated
        context.resetToConnected();
        break;
      case FAILURE:
        // User has cancelled or operation failed
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_FAILED, event.getMessage().get());
        context.resetToInitialised();
        break;
      default:
        log.warn("Unexpected message event '{}'", event.getEventType().name());
        context.resetToConnected();
    }

  }
}
