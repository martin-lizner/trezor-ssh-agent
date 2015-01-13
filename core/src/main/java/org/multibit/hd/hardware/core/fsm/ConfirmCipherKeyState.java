package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletEventType;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.messages.Success;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>State to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>State transitions based on low level message events</li>
 * </ul>
 * <p>The "confirm cipher key" state occurs in response to a CIPHER_KEY
 * message and handles the ongoing button requests, success and failure messages
 * coming from the device as it provides the encrypted/decrypted data using the
 * provided address.</p>
 *
 * @since 0.0.1
 *  
 */
public class ConfirmCipherKeyState extends AbstractHardwareWalletState {

  private static final Logger log = LoggerFactory.getLogger(ConfirmCipherKeyState.class);

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      case PIN_MATRIX_REQUEST:
        // Device is asking for a PIN matrix to be displayed (user must read the screen carefully)
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_PIN_ENTRY, event.getMessage().get());
        // Further state transitions will occur after the user has provided the PIN via the service
        break;
      case BUTTON_REQUEST:
        // Device is requesting a button press
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_BUTTON_PRESS, event.getMessage().get());
        client.buttonAck();
        break;
      case SUCCESS:
        // Device has completed the operation and provided a cipher key value
        final Success message = (Success) event.getMessage().get();
        context.setEntropy(message.getPayload());
        // Once the context is updated inform downstream consumers
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_SUCCEEDED, event.getMessage().get());
        // No reset required
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
