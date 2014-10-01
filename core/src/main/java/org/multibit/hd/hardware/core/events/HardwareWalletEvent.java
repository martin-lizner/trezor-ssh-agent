package org.multibit.hd.hardware.core.events;

import com.google.common.base.Optional;
import com.google.protobuf.Message;

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

  private final MessageType messageType;

  private final Optional<Message> message;

  /**
   * @param messageType The message type
   * @param message     The protocol buffer message from the wire
   */
  public HardwareWalletEvent(MessageType messageType, Optional<Message> message) {

    this.messageType = messageType;
    this.message = message;
  }

  /**
   * @return The protocol message type
   */
  public MessageType getMessageType() {
    return messageType;
  }

  /**
   * @return The protocol buffer message from the wire if present
   */
  public Optional<Message> getMessage() {
    return message;
  }

}
