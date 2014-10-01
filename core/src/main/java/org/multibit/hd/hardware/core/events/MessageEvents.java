package org.multibit.hd.hardware.core.events;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.messages.HardwareWalletMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Factory to provide the following to application API:</p>
 * <ul>
 * <li>Entry point to broadcast the low level message events to the service</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class MessageEvents {

  private static final Logger log = LoggerFactory.getLogger(MessageEvents.class);

  /**
   * Utilities have a private constructor
   */
  private MessageEvents() {
  }

  /**
   * <p>Convenience method to fire a message event</p>
   *
   * @param event The event (e.g. DEVICE_CONNECTED)
   */
  public static void fireMessageEvent(final MessageEvent event) {

    Preconditions.checkNotNull(event, "'messageType' must be present");

    log.debug("Firing 'message' event: {}", event.getEventType().name());
    HardwareWalletService.messageEventBus.post(event);

  }

  /**
   * <p>A message event without a protobuf message is used for communicating system status changes (e.g. DISCONNECT)</p>
   *
   * @param messageEventType The message type (e.g. DEVICE_CONNECTED)
   */
  public static void fireMessageEvent(final MessageEventType messageEventType) {

    Preconditions.checkNotNull(messageEventType, "'messageType' must be present");

    log.debug("Firing 'message' event: {}", messageEventType.name());
    HardwareWalletService.messageEventBus.post(new MessageEvent(
      messageEventType,
      Optional.<HardwareWalletMessage>absent(),
      Optional.<Message>absent()
    ));

  }

}
