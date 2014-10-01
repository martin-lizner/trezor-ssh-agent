package org.multibit.hd.hardware.core.events;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.messages.HardwareWalletMessage;
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
   * <p>A hardware event can wrap a hardware wallet message adapted from a protocol buffer message</p>
   *
   * @param eventType The event type (e.g. SHOW_DEVICE_READY)
   * @param message   The message itself (from protocol buffers)
   */
  public static void fireHardwareWalletEvent(final HardwareWalletEventType eventType, final HardwareWalletMessage message) {

    Preconditions.checkNotNull(eventType, "'messageType' must be present");
    Preconditions.checkNotNull(message, "'message' must be present");

    log.debug("Firing 'hardware wallet' event: {}", eventType.name());
    HardwareWalletService.hardwareWalletEventBus.post(new HardwareWalletEvent(
      eventType,
      Optional.of(message)
    ));

  }

  /**
   * <p>A hardware event can have no further information</p>
   *
   * @param eventType The event type (e.g. SHOW_DEVICE_READY)
   */
  public static void fireHardwareWalletEvent(final HardwareWalletEventType eventType) {

    Preconditions.checkNotNull(eventType, "'eventType' must be present");

    log.debug("Firing 'hardware wallet' event: {}", eventType.name());
    HardwareWalletService.hardwareWalletEventBus.post(new HardwareWalletEvent(
      eventType,
      Optional.<HardwareWalletMessage>absent()
    ));

  }

}
