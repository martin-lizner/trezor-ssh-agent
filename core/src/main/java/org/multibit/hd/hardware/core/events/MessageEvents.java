package org.multibit.hd.hardware.core.events;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletService;
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
   * <p>A message event usually wraps a protobuf message</p>
   *
   * @param messageType The message type (e.g. SUCCESS)
   * @param message     The message itself (from protocol buffers)
   */
  public static void fireMessageEvent(final HardwareWalletMessageType messageType, final Message message) {

    Preconditions.checkNotNull(messageType, "'messageType' must be present");
    Preconditions.checkNotNull(message, "'message' must be present");

    log.debug("Firing 'message' event: {}", messageType.name());
    HardwareWalletService.messageEventBus.post(new HardwareWalletEvent(
      messageType,
      Optional.of(message)
    ));

  }

  /**
   * <p>A message event without a protobuf message is used for communicating system status changes (e.g. DISCONNECT)</p>
   *
   * @param messageType The message type (e.g. DEVICE_CONNECTED)
   */
  public static void fireMessageEvent(final HardwareWalletMessageType messageType) {

    Preconditions.checkNotNull(messageType, "'messageType' must be present");

    log.debug("Firing 'message' event: {}", messageType.name());
    HardwareWalletService.messageEventBus.post(new HardwareWalletEvent(
      messageType,
      Optional.<Message>absent()
    ));

  }

}
