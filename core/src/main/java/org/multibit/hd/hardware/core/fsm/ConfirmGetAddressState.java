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
 * <p>The "confirm get address" state occurs in response to a GET_ADDRESS message and handles
 * the ongoing button requests, success and failure messages coming from the device as it
 * shows the address generated from the seed phrase.</p>
 *
 * @since 0.0.1
 *  
 */
public class ConfirmGetAddressState extends AbstractHardwareWalletState {

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      case BUTTON_REQUEST:
        // Device is asking for button press (address display, confirmation of reset etc)
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_BUTTON_PRESS, event.getMessage().get());
        client.buttonAck();
        break;
      case PIN_MATRIX_REQUEST:
        // Device is asking for a PIN matrix to be displayed (user must read the screen carefully)
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_PIN_ENTRY, event.getMessage().get());
        // Further state transitions will occur after the user has provided the PIN via the service
        break;
      case PASSPHRASE_REQUEST:
        // Device is asking for a passphrase screen to be displayed
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_PASSPHRASE_ENTRY);
        // Further state transitions will occur after the user has provided the passphrase via the service
        break;
      case ADDRESS:
        // Device has completed the operation and provided an address
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.ADDRESS, event.getMessage().get());
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
