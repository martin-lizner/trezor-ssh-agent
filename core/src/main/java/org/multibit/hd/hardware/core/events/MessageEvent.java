package org.multibit.hd.hardware.core.events;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.messages.HardwareWalletMessage;

/**
 * <p>Low level event to provide the following to client API:</p>
 * <ul>
 * <li>Notification of a low level hardware event</li>
 * <li>Wrapping of the message and message type to unify API for downstream consumers</li>
 * </ul>
 * <p>Messages wrap the adapted raw data from the specific hardware wallet (e.g. initialise, reset etc)</p>
 * <p>If a message is not present then the event wraps the general state of a hardware wallet (e.g. connected, disconnected etc)</p>
 *
 * @since 0.0.1
 *  
 */
public class MessageEvent {

  private final HardwareWalletMessageType messageType;

  private final Optional<HardwareWalletMessage> message;

  private final Optional<Message> rawMessage;

  /**
   * @param messageType The message type
   * @param message     The adapted hardware wallet message
   * @param rawMessage  The raw protobuf message from the hardware wallet
   */
  public MessageEvent(HardwareWalletMessageType messageType, Optional<HardwareWalletMessage> message, Optional<Message> rawMessage) {

    this.messageType = messageType;
    this.message = message;
    this.rawMessage = rawMessage;
  }

  /**
   * @return The low level message type
   */
  public HardwareWalletMessageType getMessageType() {
    return messageType;
  }

  /**
   * @return The adapted low level message if present
   */
  public Optional<HardwareWalletMessage> getMessage() {
    return message;
  }

  /**
   * @return The raw protocol buffer message from the wire if present
   */
  public Optional<Message> getRawMessage() {
    return rawMessage;
  }
}
