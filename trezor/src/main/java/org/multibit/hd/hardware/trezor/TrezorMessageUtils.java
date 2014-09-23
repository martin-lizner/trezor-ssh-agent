package org.multibit.hd.hardware.trezor;

import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

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

  private static final Map<Class<? extends Message>, TrezorMessage.MessageType> trezorMessageMap = Maps.newHashMap();

  static {


    for (TrezorMessage.MessageType trezorMessageType : TrezorMessage.MessageType.values()) {

      switch (trezorMessageType) {
        case MessageType_Initialize:
          trezorMessageMap.put(TrezorMessage.Initialize.class, trezorMessageType);
          break;
        case MessageType_Ping:
          trezorMessageMap.put(TrezorMessage.Ping.class, trezorMessageType);
          break;
        case MessageType_Success:
          trezorMessageMap.put(TrezorMessage.Success.class, trezorMessageType);
          break;
        case MessageType_Failure:
          trezorMessageMap.put(TrezorMessage.Failure.class, trezorMessageType);
          break;
        case MessageType_ChangePin:
          trezorMessageMap.put(TrezorMessage.ChangePin.class, trezorMessageType);
          break;
        case MessageType_WipeDevice:
          trezorMessageMap.put(TrezorMessage.WipeDevice.class, trezorMessageType);
          break;
        case MessageType_FirmwareErase:
          trezorMessageMap.put(TrezorMessage.FirmwareErase.class, trezorMessageType);
          break;
        case MessageType_FirmwareUpload:
          trezorMessageMap.put(TrezorMessage.FirmwareUpload.class, trezorMessageType);
          break;
        case MessageType_GetEntropy:
          trezorMessageMap.put(TrezorMessage.GetEntropy.class, trezorMessageType);
          break;
        case MessageType_Entropy:
          trezorMessageMap.put(TrezorMessage.Entropy.class, trezorMessageType);
          break;
        case MessageType_GetPublicKey:
          trezorMessageMap.put(TrezorMessage.GetPublicKey.class, trezorMessageType);
          break;
        case MessageType_PublicKey:
          trezorMessageMap.put(TrezorMessage.PublicKey.class, trezorMessageType);
          break;
        case MessageType_LoadDevice:
          trezorMessageMap.put(TrezorMessage.LoadDevice.class, trezorMessageType);
          break;
        case MessageType_ResetDevice:
          trezorMessageMap.put(TrezorMessage.ResetDevice.class, trezorMessageType);
          break;
        case MessageType_SignTx:
          trezorMessageMap.put(TrezorMessage.SignTx.class, trezorMessageType);
          break;
        case MessageType_SimpleSignTx:
          trezorMessageMap.put(TrezorMessage.SimpleSignTx.class, trezorMessageType);
          break;
        case MessageType_Features:
          trezorMessageMap.put(TrezorMessage.Features.class, trezorMessageType);
          break;
        case MessageType_PinMatrixRequest:
          trezorMessageMap.put(TrezorMessage.PinMatrixRequest.class, trezorMessageType);
          break;
        case MessageType_PinMatrixAck:
          trezorMessageMap.put(TrezorMessage.PinMatrixAck.class, trezorMessageType);
          break;
        case MessageType_Cancel:
          trezorMessageMap.put(TrezorMessage.Cancel.class, trezorMessageType);
          break;
        case MessageType_TxRequest:
          trezorMessageMap.put(TrezorMessage.TxRequest.class, trezorMessageType);
          break;
        case MessageType_TxAck:
          trezorMessageMap.put(TrezorMessage.TxAck.class, trezorMessageType);
          break;
        case MessageType_CipherKeyValue:
          trezorMessageMap.put(TrezorMessage.CipherKeyValue.class, trezorMessageType);
          break;
        case MessageType_ClearSession:
          trezorMessageMap.put(TrezorMessage.ClearSession.class, trezorMessageType);
          break;
        case MessageType_ApplySettings:
          trezorMessageMap.put(TrezorMessage.ApplySettings.class, trezorMessageType);
          break;
        case MessageType_ButtonRequest:
          trezorMessageMap.put(TrezorMessage.ButtonRequest.class, trezorMessageType);
          break;
        case MessageType_ButtonAck:
          trezorMessageMap.put(TrezorMessage.ButtonAck.class, trezorMessageType);
          break;
        case MessageType_GetAddress:
          trezorMessageMap.put(TrezorMessage.GetAddress.class, trezorMessageType);
          break;
        case MessageType_Address:
          trezorMessageMap.put(TrezorMessage.Address.class, trezorMessageType);
          break;
        case MessageType_EntropyRequest:
          trezorMessageMap.put(TrezorMessage.EntropyRequest.class, trezorMessageType);
          break;
        case MessageType_EntropyAck:
          trezorMessageMap.put(TrezorMessage.EntropyAck.class, trezorMessageType);
          break;
        case MessageType_SignMessage:
          trezorMessageMap.put(TrezorMessage.SignMessage.class, trezorMessageType);
          break;
        case MessageType_VerifyMessage:
          trezorMessageMap.put(TrezorMessage.VerifyMessage.class, trezorMessageType);
          break;
        case MessageType_MessageSignature:
          trezorMessageMap.put(TrezorMessage.MessageSignature.class, trezorMessageType);
          break;
        case MessageType_EncryptMessage:
          trezorMessageMap.put(TrezorMessage.EncryptMessage.class, trezorMessageType);
          break;
        case MessageType_DecryptMessage:
          trezorMessageMap.put(TrezorMessage.DecryptMessage.class, trezorMessageType);
          break;
        case MessageType_PassphraseRequest:
          trezorMessageMap.put(TrezorMessage.PassphraseRequest.class, trezorMessageType);
          break;
        case MessageType_PassphraseAck:
          trezorMessageMap.put(TrezorMessage.PassphraseAck.class, trezorMessageType);
          break;
        case MessageType_EstimateTxSize:
          trezorMessageMap.put(TrezorMessage.EstimateTxSize.class, trezorMessageType);
          break;
        case MessageType_TxSize:
          trezorMessageMap.put(TrezorMessage.TxSize.class, trezorMessageType);
          break;
        case MessageType_RecoveryDevice:
          trezorMessageMap.put(TrezorMessage.RecoveryDevice.class, trezorMessageType);
          break;
        case MessageType_WordRequest:
          trezorMessageMap.put(TrezorMessage.WordRequest.class, trezorMessageType);
          break;
        case MessageType_WordAck:
          trezorMessageMap.put(TrezorMessage.WordAck.class, trezorMessageType);
          break;
        case MessageType_DebugLinkDecision:
          trezorMessageMap.put(TrezorMessage.DebugLinkDecision.class, trezorMessageType);
          break;
        case MessageType_DebugLinkGetState:
          trezorMessageMap.put(TrezorMessage.DebugLinkGetState.class, trezorMessageType);
          break;
        case MessageType_DebugLinkState:
          trezorMessageMap.put(TrezorMessage.DebugLinkState.class, trezorMessageType);
          break;
        case MessageType_DebugLinkStop:
          trezorMessageMap.put(TrezorMessage.DebugLinkStop.class, trezorMessageType);
          break;
        case MessageType_DebugLinkLog:
          trezorMessageMap.put(TrezorMessage.DebugLinkLog.class, trezorMessageType);
          break;
        default:
          throw new IllegalStateException("Unknown message type: " + trezorMessageType.name());

      }

    }


  }

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

    Class clazz = message.getClass();

    TrezorMessage.MessageType messageType = trezorMessageMap.get(clazz);

    if (messageType != null) {

      return (short) messageType.getNumber();

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
