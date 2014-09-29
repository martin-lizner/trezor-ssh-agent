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
 * <li>Entry point to broadcast high level hardware wallet events to downstream consumers</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class HardwareWalletEvents {

  private static final Logger log = LoggerFactory.getLogger(HardwareWalletEvents.class);

  /**
   * Utilities have a private constructor
   */
  private HardwareWalletEvents() {
  }

  /**
   * <p>A hardware event can wrap a protocol buffer message</p>
   *
   * @param messageType The message type (e.g. SUCCESS)
   * @param message     The message itself (from protocol buffers)
   */
  public static void fireHardwareWalletEvent(final HardwareWalletMessageType messageType, final Message message) {

    Preconditions.checkNotNull(messageType, "'messageType' must be present");
    Preconditions.checkNotNull(message, "'message' must be present");

    log.debug("Firing 'hardware wallet' event: {}", messageType.name());
    HardwareWalletService.hardwareWalletEventBus.post(new HardwareWalletEvent(
      messageType,
      Optional.of(message)
    ));

  }

  /**
   * <p>A hardware event without a message is used for communicating system status changes (e.g. DISCONNECT)</p>
   *
   * @param messageType The message type (e.g. SUCCESS)
   */
  public static void fireHardwareWalletEvent(final HardwareWalletMessageType messageType) {

    Preconditions.checkNotNull(messageType, "'messageType' must be present");

    log.debug("Firing 'hardware wallet' event: {}", messageType.name());
    HardwareWalletService.hardwareWalletEventBus.post(new HardwareWalletEvent(
      messageType,
      Optional.<Message>absent()
    ));

  }

}
