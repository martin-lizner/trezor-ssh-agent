package org.multibit.hd.hardware.trezor.utils;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

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
   * @param type   The message type
   * @param buffer The buffer containing the protobuf message
   *
   * @return The message if it could be parsed
   */
  public static Message parse(TrezorMessage.MessageType type, byte[] buffer) {

    log.info("Parsing '{}' ({} bytes):", type, buffer.length);

    logPacket("<>", 0, buffer);

    try {
      Message message;

      switch (type) {
        case MessageType_Initialize:
          message = TrezorMessage.Initialize.parseFrom(buffer);
          break;
        case MessageType_Ping:
          message = TrezorMessage.Ping.parseFrom(buffer);
          break;
        case MessageType_Success:
          message = TrezorMessage.Success.parseFrom(buffer);
          break;
        case MessageType_Failure:
          message = TrezorMessage.Failure.parseFrom(buffer);
          break;
        case MessageType_ChangePin:
          message = TrezorMessage.ChangePin.parseFrom(buffer);
          break;
        case MessageType_WipeDevice:
          message = TrezorMessage.WipeDevice.parseFrom(buffer);
          break;
        case MessageType_FirmwareErase:
          message = TrezorMessage.FirmwareErase.parseFrom(buffer);
          break;
        case MessageType_FirmwareUpload:
          message = TrezorMessage.FirmwareUpload.parseFrom(buffer);
          break;
        case MessageType_GetEntropy:
          message = TrezorMessage.GetEntropy.parseFrom(buffer);
          break;
        case MessageType_Entropy:
          message = TrezorMessage.Entropy.parseFrom(buffer);
          break;
        case MessageType_GetPublicKey:
          message = TrezorMessage.GetPublicKey.parseFrom(buffer);
          break;
        case MessageType_PublicKey:
          message = TrezorMessage.PublicKey.parseFrom(buffer);
          break;
        case MessageType_LoadDevice:
          message = TrezorMessage.LoadDevice.parseFrom(buffer);
          break;
        case MessageType_ResetDevice:
          message = TrezorMessage.ResetDevice.parseFrom(buffer);
          break;
        case MessageType_SignTx:
          message = TrezorMessage.SignTx.parseFrom(buffer);
          break;
        case MessageType_SimpleSignTx:
          message = TrezorMessage.SimpleSignTx.parseFrom(buffer);
          break;
        case MessageType_Features:
          message = TrezorMessage.Features.parseFrom(buffer);
          break;
        case MessageType_PinMatrixRequest:
          message = TrezorMessage.PinMatrixRequest.parseFrom(buffer);
          break;
        case MessageType_PinMatrixAck:
          message = TrezorMessage.PinMatrixAck.parseFrom(buffer);
          break;
        case MessageType_Cancel:
          message = TrezorMessage.Cancel.parseFrom(buffer);
          break;
        case MessageType_TxRequest:
          message = TrezorMessage.TxRequest.parseFrom(buffer);
          break;
        case MessageType_TxAck:
          message = TrezorMessage.TxAck.parseFrom(buffer);
          break;
        case MessageType_CipherKeyValue:
          message = TrezorMessage.CipherKeyValue.parseFrom(buffer);
          break;
        case MessageType_ClearSession:
          message = TrezorMessage.ClearSession.parseFrom(buffer);
          break;
        case MessageType_ApplySettings:
          message = TrezorMessage.ApplySettings.parseFrom(buffer);
          break;
        case MessageType_ButtonRequest:
          message = TrezorMessage.ButtonRequest.parseFrom(buffer);
          break;
        case MessageType_ButtonAck:
          message = TrezorMessage.ButtonAck.parseFrom(buffer);
          break;
        case MessageType_GetAddress:
          message = TrezorMessage.GetAddress.parseFrom(buffer);
          break;
        case MessageType_Address:
          message = TrezorMessage.Address.parseFrom(buffer);
          break;
        case MessageType_EntropyRequest:
          message = TrezorMessage.EntropyRequest.parseFrom(buffer);
          break;
        case MessageType_EntropyAck:
          message = TrezorMessage.EntropyAck.parseFrom(buffer);
          break;
        case MessageType_SignMessage:
          message = TrezorMessage.SignMessage.parseFrom(buffer);
          break;
        case MessageType_VerifyMessage:
          message = TrezorMessage.VerifyMessage.parseFrom(buffer);
          break;
        case MessageType_MessageSignature:
          message = TrezorMessage.MessageSignature.parseFrom(buffer);
          break;
        case MessageType_EncryptMessage:
          message = TrezorMessage.EncryptMessage.parseFrom(buffer);
          break;
        case MessageType_DecryptMessage:
          message = TrezorMessage.DecryptMessage.parseFrom(buffer);
          break;
        case MessageType_PassphraseRequest:
          message = TrezorMessage.PassphraseRequest.parseFrom(buffer);
          break;
        case MessageType_PassphraseAck:
          message = TrezorMessage.PassphraseAck.parseFrom(buffer);
          break;
        case MessageType_EstimateTxSize:
          message = TrezorMessage.EstimateTxSize.parseFrom(buffer);
          break;
        case MessageType_TxSize:
          message = TrezorMessage.TxSize.parseFrom(buffer);
          break;
        case MessageType_RecoveryDevice:
          message = TrezorMessage.RecoveryDevice.parseFrom(buffer);
          break;
        case MessageType_WordRequest:
          message = TrezorMessage.WordRequest.parseFrom(buffer);
          break;
        case MessageType_WordAck:
          message = TrezorMessage.WordAck.parseFrom(buffer);
          break;
        case MessageType_DebugLinkDecision:
          message = TrezorMessage.DebugLinkDecision.parseFrom(buffer);
          break;
        case MessageType_DebugLinkGetState:
          message = TrezorMessage.DebugLinkGetState.parseFrom(buffer);
          break;
        case MessageType_DebugLinkState:
          message = TrezorMessage.DebugLinkState.parseFrom(buffer);
          break;
        case MessageType_DebugLinkStop:
          message = TrezorMessage.DebugLinkStop.parseFrom(buffer);
          break;
        case MessageType_DebugLinkLog:
          message = TrezorMessage.DebugLinkLog.parseFrom(buffer);
          break;
        default:
          throw new IllegalStateException("Unknown message type: " + type.name());
      }

      // Must be OK to be here
      log.debug("< Message: {}", ToStringBuilder.reflectionToString(message, ToStringStyle.MULTI_LINE_STYLE));

      return message;

    } catch (InvalidProtocolBufferException e) {
      log.error("Could not parse message", e);
    }

    // Must have failed to be here
    return null;

  }

  /**
   * @param prefix The logging prefix (usually ">" for write and "<" for read)
   * @param count  The packet count
   * @param buffer The buffer containing the packet to log
   */
  public static void logPacket(String prefix, int count, byte[] buffer) {

    // Only do work if required
    if (log.isDebugEnabled()) {
      String s = prefix + " Packet [" + count + "]:";
      for (byte b : buffer) {
        s += String.format(" %02x", b);
      }
      log.debug("{}", s);
    }

  }

  /**
   * <p>Write a Trezor protocol buffer message to an OutputStream</p>
   *
   * @param message The protocol buffer message to read
   * @param out     The data output stream (must be open)
   *
   * @throws java.io.IOException If the device disconnects during IO
   */

  public static void writeAsHIDPackets(Message message, OutputStream out) throws IOException {

    // The message presented as a collection of HID packets
    ByteBuffer messageBuffer = formatAsHIDPackets(message);

    int packets = messageBuffer.position() / 63;
    log.info("Writing {} packets", packets);
    messageBuffer.rewind();

    // HID requires 64 byte packets with 63 bytes of payload
    for (int i = 0; i < packets; i++) {

      byte[] buffer = new byte[64];
      buffer[0] = 63; // Length
      messageBuffer.get(buffer, 1, 63); // Payload

      // Describe the packet
      String s = "Packet [" + i + "]: ";
      for (int j = 0; j < 64; j++) {
        s += String.format(" %02x", buffer[j]);
      }

      log.info("> {}", s);

      out.write(buffer);

      // Flush to ensure bytes are available immediately
      out.flush();

    }

  }

  /**
   * <p>Format a Trezor protobuf message as a byte buffer filled with HID packets</p>
   *
   * @param message The Trezor protobuf message
   *
   * @return A byte buffer containing a set of HID packets
   */
  public static ByteBuffer formatAsHIDPackets(Message message) {

    int msgSize = message.getSerializedSize();
    String msgName = message.getClass().getSimpleName();
    int msgId = TrezorMessage.MessageType.valueOf("MessageType_" + msgName).getNumber();

    log.debug("Formatting message: '{}' ({} bytes)", msgName, msgSize);

    // Create the header
    ByteBuffer messageBuffer = ByteBuffer.allocate(32768);

    // Marker bytes
    messageBuffer.put((byte) '#');
    messageBuffer.put((byte) '#');

    // Header code
    messageBuffer.put((byte) ((msgId >> 8) & 0xFF));
    messageBuffer.put((byte) (msgId & 0xFF));

    // Message size
    messageBuffer.put((byte) ((msgSize >> 24) & 0xFF));
    messageBuffer.put((byte) ((msgSize >> 16) & 0xFF));
    messageBuffer.put((byte) ((msgSize >> 8) & 0xFF));
    messageBuffer.put((byte) (msgSize & 0xFF));

    // Message payload
    messageBuffer.put(message.toByteArray());

    // Packet padding
    while (messageBuffer.position() % 63 > 0) {
      messageBuffer.put((byte) 0);
    }

    return messageBuffer;
  }

  /**
   * <p>Parse the contents of the input stream into a Trezor protobuf message</p>
   *
   * @param in The input stream containing Trezor HID packets
   *
   * @return A message
   */
  public static Message parseAsHIDPackets(InputStream in) throws IOException {

    ByteBuffer messageBuffer = ByteBuffer.allocate(32768);

    TrezorMessage.MessageType type;
    int msgSize;
    int received;

    // Keep reading until synchronized on "##"
    for (; ; ) {
      byte[] buffer = new byte[64];

      received = in.read(buffer);

      log.debug("< {} bytes", received);
      TrezorMessageUtils.logPacket("<", 0, buffer);

      if (received < 9) {
        continue;
      }

      // Synchronize the buffer on start of new message ('?' is ASCII 63)
      if (buffer[0] != (byte) '?' || buffer[1] != (byte) '#' || buffer[2] != (byte) '#') {
        // Reject packet
        continue;
      }

      // Evaluate the header information (short, int)
      type = TrezorMessage.MessageType.valueOf((buffer[3] << 8 & 0xFF) + buffer[4]);
      msgSize = ((buffer[5] & 0xFF) << 24) + ((buffer[6] & 0xFF) << 16) + ((buffer[7] & 0xFF) << 8) + (buffer[8] & 0xFF);

      // Treat remainder of packet as the protobuf message payload
      messageBuffer.put(buffer, 9, buffer.length - 9);

      break;
    }

    log.debug("< Type: '{}' Message size: '{}' bytes", type.name(), msgSize);

    int packet = 0;
    while (messageBuffer.position() < msgSize) {

      byte[] buffer = new byte[64];
      received = in.read(buffer);
      packet++;

      log.debug("< (cont) {} bytes", received);
      TrezorMessageUtils.logPacket("<", packet, buffer);

      if (buffer[0] != (byte) '?') {
        log.warn("< Malformed packet length. Expected: '3f' Actual: '{}'. Ignoring.", String.format("%02x", buffer[0]));
        continue;
      }

      // Append the packet payload to the message buffer
      messageBuffer.put(buffer, 1, buffer.length - 1);
    }

    log.debug("Packet complete");

    // Parse the message
    return TrezorMessageUtils.parse(type, Arrays.copyOfRange(messageBuffer.array(), 0, msgSize));

  }
}
