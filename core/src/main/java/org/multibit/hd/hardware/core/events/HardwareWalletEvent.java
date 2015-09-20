package org.multibit.hd.hardware.core.events;

import com.google.common.base.Optional;
import org.multibit.hd.hardware.core.messages.HardwareWalletMessage;

/**
 * <p>High level event to provide the following to downstream consumers:</p>
 * <ul>
 * <li>Notification of a hardware wallet state change (connection, completed Tx signing etc)</li>
 * </ul>
 * <p>Hardware wallet events provide a suitably abstracted view of the wallet to allow downstream consumers to concentrate on
 * user interface changes as a result of the current state of the hardware wallet. For example, if the device is awaiting a
 * button press the downstream UI should present a popup dialog indicating that the user should look to their device.</p>
 *
 * @since 0.0.1
 *  
 */
public class HardwareWalletEvent {

  private final HardwareWalletEventType eventType;
  private final Optional<HardwareWalletMessage> message;
  private final String source;

  /**
   * @param eventType The hardware wallet event type
   * @param message   The hardware wallet message adapted from the wire
   * @param source    The client name acting as the source (e.g. "TREZOR", "KEEP_KEY" etc)
   */
  public HardwareWalletEvent(HardwareWalletEventType eventType, Optional<HardwareWalletMessage> message, String source) {

    this.eventType = eventType;
    this.message = message;
    this.source = source;

  }

  /**
   * @return The protocol message type
   */
  public HardwareWalletEventType getEventType() {
    return eventType;
  }

  /**
   * @return The hardware wallet message adapted from the wire if present
   */
  public Optional<HardwareWalletMessage> getMessage() {
    return message;
  }

  /**
   * @return The client name acting as the source (e.g. "TREZOR", "KEEP_KEY" etc)
   */
  public String getSource() {
    return source;
  }
}
