package org.multibit.hd.hardware.core.events;

import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.messages.ProtocolMessageType;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Factory to provide the following to application API:</p>
 * <ul>
 * <li>Entry point to broadcast org.multibit.hd.hardware.trezor.core events</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class HardwareEvents {

  private static final Logger log = LoggerFactory.getLogger(HardwareEvents.class);

  /**
   * Utilities have a private constructor
   */
  private HardwareEvents() {
  }

  /**
   * <p>A system event is one that falls outside of the hardware communications protocol (i.e. a DISCONNECTED or similar)</p>
   *
   * @param messageType The message type (e.g. DISCONNECT)
   */
  public static void fireSystemEvent(final SystemMessageType messageType) {

    Preconditions.checkNotNull(messageType, "'messageType' must be present");

    log.debug("Firing 'hardware wallet system' event");
    HardwareWalletService.hardwareEventBus.post(new HardwareWalletSystemEvent(messageType));

  }

  /**
   * <p>A protocol event is one that falls within the hardware communications protocol (i.e. a PING or similar)</p>
   *
   * @param messageType The message type (e.g. SUCCESS)
   * @param message     The message itself (from protocol buffers)
   */
  public static void fireProtocolEvent(final ProtocolMessageType messageType, final Message message) {

    Preconditions.checkNotNull(messageType, "'messageType' must be present");
    Preconditions.checkNotNull(message, "'message' must be present");

    log.debug("Firing 'hardware wallet protocol' event");
    HardwareWalletService.hardwareEventBus.post(new HardwareWalletProtocolEvent(
      messageType,
      message
    ));

  }
}
