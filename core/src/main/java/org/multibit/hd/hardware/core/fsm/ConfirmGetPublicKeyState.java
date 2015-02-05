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
 * <p>The "confirm get public key" state occurs in response to a GET_PUBLIC_KEY
 * message and handles the ongoing button requests, success and failure messages
 * coming from the device as it provides the public key generated from the seed phrase.</p>
 *
 * @since 0.0.1
 *  
 */
public class ConfirmGetPublicKeyState extends AbstractHardwareWalletState {

  private static final Logger log = LoggerFactory.getLogger(ConfirmGetPublicKeyState.class);

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      case PUBLIC_KEY:
        // Device has completed the operation and provided a public key
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.PUBLIC_KEY, event.getMessage().get());
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
