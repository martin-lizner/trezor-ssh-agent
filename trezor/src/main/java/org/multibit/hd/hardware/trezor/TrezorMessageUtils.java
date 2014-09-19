package org.multibit.hd.hardware.trezor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>Utility class to provide the following to applications:</p>
 * <ul>
 * <li>Various TrezorMessage related operations</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public final class TrezorMessageUtils {

  private static final Logger log = LoggerFactory.getLogger(TrezorMessageUtils.class);

    /**
     * Utilities should not have public constructors
     */
    private TrezorMessageUtils() {
    }

    /**
     * <p>Write a Trezor protocol buffer message to an OutputStream</p>
     *
     * @param message The protocol buffer message to read
     * @param out     The data output stream (must be open)
     *
     * @throws java.io.IOException If the device disconnects during IO
     */

  public static void writeMessage(Message message, DataOutputStream out) throws IOException {

    // Require the header code
    short headerCode = getHeaderCode(message);

    // Provide some debugging
    TrezorMessage.MessageType messageType = getMessageTypeByHeaderCode(headerCode);
    log.debug("> {}", messageType.name());

    // Write magic alignment string (avoiding immediate flush)
    out.writeBytes("##");

    // Write header following Python's ">HL" syntax
    // > = Big endian, std size and alignment
    // H = Unsigned short (2 bytes) for header code
    // L = Unsigned long (4 bytes) for message length

    // Message type
    out.writeShort(headerCode);

    // Message length
    out.writeInt(message.getSerializedSize());

    // Write the detail portion as a protocol buffer message
    message.writeTo(out);

    // Flush to ensure bytes are available immediately
    out.flush();

  }

  /**
   * @param headerCode The header code (e.g. "0" for INITIALIZE)
   *
   * @return The matching message
   *
   * @throws IllegalArgumentException If the header code is not valid
   */
  public static TrezorMessage.MessageType getMessageTypeByHeaderCode(short headerCode) {

    return TrezorMessage.MessageType.valueOf(headerCode);

  }

  /**
   * @param message The protocol buffer message class (e.g. "Initialize")
   *
   * @return The header code for use with the Trezor header section
   *
   * @throws IllegalArgumentException If the message is not valid
   */
  public static short getHeaderCode(Message message) {

    for (TrezorMessage.MessageType trezorMessageType : TrezorMessage.MessageType.values()) {

      // Check for same type
      if (trezorMessageType.getClass().equals(message.getClass())) {
        return (short) trezorMessageType.getNumber();
      }
    }

    throw new IllegalArgumentException("Message class '" + message.getClass().getName() + "' is not known");

  }

  /**
   * @param headerCode The header code (e.g. "0" for INITIALIZE)
   * @param bytes      The bytes forming the message
   *
   * @return A protocol buffer Message derived from the bytes
   *
   * @throws InvalidProtocolBufferException If the bytes cannot be merged
   */
  public static Message parse(Short headerCode, byte[] bytes) throws InvalidProtocolBufferException {

    // Identify the message type from the header code
    TrezorMessage.MessageType messageType = getMessageTypeByHeaderCode(headerCode);

    // Use a default instance to merge the bytes
    return messageType
      .getDescriptorForType()
      .toProto()
      .getDefaultInstanceForType()
      .newBuilderForType()
      .mergeFrom(bytes)
      .build();

  }

}
