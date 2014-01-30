package org.multibit.hd.hardware.trezor.core.events;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.trezor.core.TrezorEvent;
import org.multibit.hd.hardware.trezor.core.TrezorEventType;
import org.multibit.hd.hardware.trezor.core.protobuf.MessageType;

/**
 * <p>Factory to provide the following to event producers:</p>
 * <ul>
 * <li>New instances of standard Trezor events</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public class TrezorEvents {

  /**
   * Utilities have no public constructor
   */
  private TrezorEvents() {
  }

  /**
   * <p>A protocol event is one that falls within the Trezor communications protocol (i.e. not a DISCONNECTED or similar)</p>
   *
   * @param messageType The message type (e.g. SUCCESS)
   * @param message     The message itself (from protocol buffers)
   *
   * @return A new immutable event instance
   */
  public static TrezorEvent newProtocolEvent(final MessageType messageType, final Message message) {

    return new TrezorEvent() {

      @Override
      public Optional<Message> protocolMessage() {
        return Optional.fromNullable(message);
      }

      @Override
      public Optional<MessageType> protocolMessageType() {
        return Optional.fromNullable(messageType);
      }

      @Override
      public TrezorEventType eventType() {
        return TrezorEventType.PROTOCOL_MESSAGE;
      }
    };

  }

  /**
   * <p>A system event is one that falls outside of the Trezor communications protocol (i.e. a DISCONNECTED or similar)</p>
   *
   * @param trezorEventType The event type (e.g. DISCONNECTED)
   * @return A new immutable event instance
   */
  public static TrezorEvent newSystemEvent(final TrezorEventType trezorEventType) {

    Preconditions.checkNotNull(trezorEventType);

    return new TrezorEvent() {

      @Override
      public Optional<Message> protocolMessage() {
        return Optional.absent();
      }

      @Override
      public Optional<MessageType> protocolMessageType() {
        return Optional.absent();
      }

      @Override
      public TrezorEventType eventType() {
        return trezorEventType;
      }
    };

  }


}
