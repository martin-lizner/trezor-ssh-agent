package org.multibit.hd.hardware.core.events;

import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.messages.ProtocolMessageType;

/**
 * <p>Event to provide the following to application API:</p>
 * <ul>
 * <li>Notification of a hardware event at the protocol layer</li>
 * </ul>
 * <p>Protocol messages wrap the raw data from the specific hardware wallet</p>
 *
 * @since 0.0.1
 *  
 */
public class HardwareWalletProtocolEvent {

  private final ProtocolMessageType messageType;
  private final Message message;

  /**
   * @param messageType The protocol message type
   * @param message     The protocol buffer message from the wire
   */
  public HardwareWalletProtocolEvent(ProtocolMessageType messageType, Message message) {

    this.messageType = messageType;
    this.message = message;
  }

  /**
   * @return The protocol message type
   */
  public ProtocolMessageType getMessageType() {
    return messageType;
  }

  /**
   * @return The protocol buffer message from the wire
   */
  public Message getMessage() {
    return message;
  }

}
