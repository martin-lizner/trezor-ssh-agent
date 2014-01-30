package org.multibit.hd.hardware.trezor.core;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.trezor.core.protobuf.MessageType;

/**
 * <p>Interface to provide the following to application:</p>
 * <ul>
 * <li>Identification of the underlying protocol buffer message for an event</li>
 * <li>The event type</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public interface TrezorEvent {

  /**
   * @return The protocol buffer message that backs this event (if present)
   */
  Optional<Message> protocolMessage();

  /**
   * @return The protocol message type (e.g. TX_INPUT etc) extracted from the header (if present)
   */
  Optional<MessageType> protocolMessageType();

  /**
   * @return The overall type (e.g. TX_INPUT etc) extracted from the header (never null)
   */
  TrezorEventType eventType();

}
