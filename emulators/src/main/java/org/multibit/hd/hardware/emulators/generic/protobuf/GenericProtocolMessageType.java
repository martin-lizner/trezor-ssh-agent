package org.multibit.hd.hardware.emulators.generic.protobuf;

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
public enum GenericProtocolMessageType {

  INITALIZE(GenericMessage.Initialize.getDefaultInstance(), (short) 0),

  PING(GenericMessage.Ping.getDefaultInstance(), (short) 1),

  SUCCESS(GenericMessage.Success.getDefaultInstance(), (short) 2),
  FAILURE(GenericMessage.Failure.getDefaultInstance(), (short) 3),

  GET_UUID(GenericMessage.GetUUID.getDefaultInstance(), (short) 4),
  UUID(GenericMessage.UUID.getDefaultInstance(), (short) 5),

  OTP_REQUEST(GenericMessage.OtpRequest.getDefaultInstance(), (short) 6),
  OTP_ACK(GenericMessage.OtpAck.getDefaultInstance(), (short) 7),
  OTP_CANCEL(GenericMessage.OtpCancel.getDefaultInstance(), (short) 8),

  GET_ENTROPY(GenericMessage.GetEntropy.getDefaultInstance(), (short) 9),
  ENTROPY(GenericMessage.Entropy.getDefaultInstance(), (short) 10),

  GET_MASTER_PUBLIC_KEY(GenericMessage.GetMasterPublicKey.getDefaultInstance(), (short) 11),
  MASTER_PUBLIC_KEY(GenericMessage.MasterPublicKey.getDefaultInstance(), (short) 12),

  LOAD_DEVICE(GenericMessage.LoadDevice.getDefaultInstance(), (short) 13),
  RESET_DEVICE(GenericMessage.ResetDevice.getDefaultInstance(), (short) 14),

  SIGN_TX(GenericMessage.SignTx.getDefaultInstance(), (short) 15),
  FEATURES(GenericMessage.Features.getDefaultInstance(), (short) 17),

  // PIN
  PIN_REQUEST(GenericMessage.PinRequest.getDefaultInstance(), (short) 18),
  PIN_ACK(GenericMessage.PinAck.getDefaultInstance(), (short) 19),
  PIN_CANCEL(GenericMessage.PinCancel.getDefaultInstance(), (short) 20),

  // Transactions
  TX_REQUEST(GenericMessage.TxRequest.getDefaultInstance(), (short) 21),
  // OUTPUT_REQUEST(Message.OutputRequest.getDefaultInstance(),(short)22),
  TX_INPUT(GenericMessage.TxInput.getDefaultInstance(), (short) 23),
  TX_OUTPUT(GenericMessage.TxOutput.getDefaultInstance(), (short) 24),
  SET_MAX_FEE_KB(GenericMessage.SetMaxFeeKb.getDefaultInstance(), (short) 25),

  // Buttons
  BUTTON_REQUEST(GenericMessage.ButtonRequest.getDefaultInstance(), (short) 26),
  BUTTON_ACK(GenericMessage.ButtonAck.getDefaultInstance(), (short) 27),
  BUTTON_CANCEL(GenericMessage.ButtonCancel.getDefaultInstance(), (short) 28),

  // Address
  GET_ADDRESS(GenericMessage.GetAddress.getDefaultInstance(), (short) 29),
  ADDRESS(GenericMessage.Address.getDefaultInstance(), (short) 30),

  // Debugging messages
  DEBUG_LINK_DECISION(GenericMessage.DebugLinkDecision.getDefaultInstance(), (short) 100),
  DEBUG_LINK_GET_STATE(GenericMessage.DebugLinkGetState.getDefaultInstance(), (short) 101),
  DEBUG_LINK_STATE(GenericMessage.DebugLinkState.getDefaultInstance(), (short) 102),
  DEBUG_LINK_STOP(GenericMessage.DebugLinkStop.getDefaultInstance(), (short) 103),

  // End of enum
  ;

  private final Message message;
  private final short headerCode;

  /**
   * @param message    The protocol buffer message class
   * @param headerCode The header code
   */
  GenericProtocolMessageType(Message message, short headerCode) {

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
  public static GenericProtocolMessageType getMessageTypeByHeaderCode(short headerCode) {

    for (GenericProtocolMessageType messageType : GenericProtocolMessageType.values()) {

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

    for (GenericProtocolMessageType messageType : GenericProtocolMessageType.values()) {

      // Check for same type
      if (messageType.getDefaultInstance().getClass().equals(trezorMessage.getClass())) {
        return messageType.getHeaderCode();
      }
    }

    throw new IllegalArgumentException("Message class '" + trezorMessage.getClass().getName() + "' is not known");

  }

  public static Message parse(Short headerCode, byte[] bytes) throws InvalidProtocolBufferException {

    GenericProtocolMessageType messageType = getMessageTypeByHeaderCode(headerCode);
    return messageType.getDefaultInstance().newBuilderForType().mergeFrom(bytes).build();

  }
}
