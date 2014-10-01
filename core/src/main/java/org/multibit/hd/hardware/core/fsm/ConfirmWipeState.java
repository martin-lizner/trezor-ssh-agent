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
 * <p>The "confirm wipe" state occurs in response to a WIPE message.</p>
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
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_BUTTON_PRESS);
        client.buttonAck();
        break;
      case FAILURE:
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_FAILED, event.getMessage().get());
        context.resetToInitialised();
        break;
      default:
        log.info("Unexpected message event '{}'", event.getEventType().name());
        context.resetToConnected();
    }

  }
}
