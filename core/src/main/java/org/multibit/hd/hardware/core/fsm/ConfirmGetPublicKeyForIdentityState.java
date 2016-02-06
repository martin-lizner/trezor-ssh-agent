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
 * <p>The "confirm get public key for identity" state occurs in response to a GET_PUBLIC_KEY
 * message and handles the ongoing button requests, success and failure messages
 * coming from the device as it provides the public key generated from the seed phrase.</p>
 *
 * @since 0.8.0
 *  
 */
public class ConfirmGetPublicKeyForIdentityState extends AbstractHardwareWalletState {

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      case PIN_MATRIX_REQUEST:
        // Device is asking for a PIN matrix to be displayed (user must read the screen carefully)
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_PIN_ENTRY, event.getMessage().get(), client.name());
        // Further state transitions will occur after the user has provided the PIN via the service
        break;
      case PASSPHRASE_REQUEST:
        // Device is asking for a passphrase screen to be displayed
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_PASSPHRASE_ENTRY, client.name());
        // Further state transitions will occur after the user has provided the passphrase via the service
        break;
      case PUBLIC_KEY:
        // Fall through since they are the same data structure
      case PUBLIC_KEY_FOR_IDENTITY:
        // Device has completed the operation and provided a public key for an identity
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.PUBLIC_KEY_FOR_IDENTITY, event.getMessage().get(), client.name());
        break;
      case FAILURE:
        // User has cancelled or operation failed
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_FAILED, event.getMessage().get(), client.name());
        context.resetToInitialised();
        break;
      default:
        handleUnexpectedMessageEvent(context, event);
    }

  }
}
