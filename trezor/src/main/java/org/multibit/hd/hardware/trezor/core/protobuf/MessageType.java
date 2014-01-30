package org.multibit.hd.hardware.trezor.core.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * <p>Enum to provide the following to application:</p>
 * <ul>
 * <li>Identification of messages and their header codes</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public enum MessageType {

  INITALIZE(TrezorMessage.Initialize.getDefaultInstance(), (short) 0),

  PING(TrezorMessage.Ping.getDefaultInstance(), (short) 1),

  SUCCESS(TrezorMessage.Success.getDefaultInstance(), (short) 2),
  FAILURE(TrezorMessage.Failure.getDefaultInstance(), (short) 3),

  GET_UUID(TrezorMessage.GetUUID.getDefaultInstance(), (short) 4),
  UUID(TrezorMessage.UUID.getDefaultInstance(), (short) 5),

  OTP_REQUEST(TrezorMessage.OtpRequest.getDefaultInstance(), (short) 6),
  OTP_ACK(TrezorMessage.OtpAck.getDefaultInstance(), (short) 7),
  OTP_CANCEL(TrezorMessage.OtpCancel.getDefaultInstance(), (short) 8),

  GET_ENTROPY(TrezorMessage.GetEntropy.getDefaultInstance(), (short) 9),
  ENTROPY(TrezorMessage.Entropy.getDefaultInstance(), (short) 10),

  GET_MASTER_PUBLIC_KEY(TrezorMessage.GetMasterPublicKey.getDefaultInstance(), (short) 11),
  MASTER_PUBLIC_KEY(TrezorMessage.MasterPublicKey.getDefaultInstance(), (short) 12),

  LOAD_DEVICE(TrezorMessage.LoadDevice.getDefaultInstance(), (short) 13),
  RESET_DEVICE(TrezorMessage.ResetDevice.getDefaultInstance(), (short) 14),

  SIGN_TX(TrezorMessage.SignTx.getDefaultInstance(), (short) 15),
  // SIGNED_TX(Message.SignedTx.getDefaultInstance(),(short)16),
  FEATURES(TrezorMessage.Features.getDefaultInstance(), (short) 17),

  // PIN
  PIN_REQUEST(TrezorMessage.PinRequest.getDefaultInstance(), (short) 18),
  PIN_ACK(TrezorMessage.PinAck.getDefaultInstance(), (short) 19),
  PIN_CANCEL(TrezorMessage.PinCancel.getDefaultInstance(), (short) 20),

  // Transactions
  TX_REQUEST(TrezorMessage.TxRequest.getDefaultInstance(), (short) 21),
  // OUTPUT_REQUEST(Message.OutputRequest.getDefaultInstance(),(short)22),
  TX_INPUT(TrezorMessage.TxInput.getDefaultInstance(), (short) 23),
  TX_OUTPUT(TrezorMessage.TxOutput.getDefaultInstance(), (short) 24),
  SET_MAX_FEE_KB(TrezorMessage.SetMaxFeeKb.getDefaultInstance(), (short) 25),

  // Buttons
  BUTTON_REQUEST(TrezorMessage.ButtonRequest.getDefaultInstance(), (short) 26),
  BUTTON_ACK(TrezorMessage.ButtonAck.getDefaultInstance(), (short) 27),
  BUTTON_CANCEL(TrezorMessage.ButtonCancel.getDefaultInstance(), (short) 28),

  // Address
  GET_ADDRESS(TrezorMessage.GetAddress.getDefaultInstance(), (short) 29),
  ADDRESS(TrezorMessage.Address.getDefaultInstance(), (short) 30),

  // Debugging messages
  DEBUG_LINK_DECISION(TrezorMessage.DebugLinkDecision.getDefaultInstance(), (short) 100),
  DEBUG_LINK_GET_STATE(TrezorMessage.DebugLinkGetState.getDefaultInstance(), (short) 101),
  DEBUG_LINK_STATE(TrezorMessage.DebugLinkState.getDefaultInstance(), (short) 102),
  DEBUG_LINK_STOP(TrezorMessage.DebugLinkStop.getDefaultInstance(), (short) 103),

  // End of enum
  ;

  private final Message message;
  private final short headerCode;

  /**
   * @param message The protocol buffer message class
   * @param headerCode      The header code
   */
  MessageType(Message message, short headerCode) {

    this.message = message;
    this.headerCode = headerCode;

  }

  /**
   * @return The default instance of the protocol buffer message (for internal use)
   */
  private Message getDefaultInstance() {
    return message;
  }

  /**
   * @return The header code to identify the message
   */
  public short getHeaderCode() {
    return headerCode;
  }

  /**
   * @param headerCode The header code (e.g. "0" for INITIALIZE)
   *
   * @return The matching message
   *
   * @throws IllegalArgumentException If the header code is not valid
   */
  public static MessageType getMessageTypeByHeaderCode(short headerCode) {

    for (MessageType messageType : MessageType.values()) {

      if (messageType.getHeaderCode() == headerCode) {
        return messageType;
      }
    }

    throw new IllegalArgumentException("Header code '" + headerCode + "' is not valid");

  }

  /**
   * @param trezorMessage The protocol buffer message class (e.g. "Message.Initialize")
   *
   * @return The unsigned short for use with the Trezor header section
   *
   * @throws IllegalArgumentException If the message is not valid
   */
  public static short getHeaderCode(Message trezorMessage) {

    for (MessageType messageType : MessageType.values()) {

      // Check for same type
      if (messageType.getDefaultInstance().getClass().equals(trezorMessage.getClass())) {
        return messageType.getHeaderCode();
      }
    }

    throw new IllegalArgumentException("Message class '" + trezorMessage.getClass().getName() + "' is not known");

  }

  public static Message parse(Short headerCode, byte[] bytes) throws InvalidProtocolBufferException {

    MessageType messageType = getMessageTypeByHeaderCode(headerCode);
    return messageType.getDefaultInstance().newBuilderForType().mergeFrom(bytes).build();

  }
}
