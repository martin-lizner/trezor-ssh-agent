package org.multibit.hd.hardware.trezor.protobuf;

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
public enum TrezorProtocolMessageType {

  // Connection
  INITALIZE(TrezorMessage.Initialize.getDefaultInstance(), (short) 0),
  PING(TrezorMessage.Ping.getDefaultInstance(), (short) 1),

  // Generic responses
  SUCCESS(TrezorMessage.Success.getDefaultInstance(), (short) 2),
  FAILURE(TrezorMessage.Failure.getDefaultInstance(), (short) 3),

  // Setup
  CHANGE_PIN(TrezorMessage.ChangePin.getDefaultInstance(), (short) 4),
  WIPE_DEVICE(TrezorMessage.WipeDevice.getDefaultInstance(), (short) 5),
  FIRMWARE_ERASE(TrezorMessage.FirmwareErase.getDefaultInstance(), (short) 6),
  FIRMWARE_UPLOAD(TrezorMessage.FirmwareUpload.getDefaultInstance(), (short) 7),

  // Entropy
  GET_ENTROPY(TrezorMessage.GetEntropy.getDefaultInstance(), (short) 9),
  ENTROPY(TrezorMessage.Entropy.getDefaultInstance(), (short) 10),

  // Key passing
  GET_PUBLIC_KEY(TrezorMessage.GetPublicKey.getDefaultInstance(), (short) 11),
  PUBLIC_KEY(TrezorMessage.PublicKey.getDefaultInstance(), (short) 12),

  // Load and reset
  LOAD_DEVICE(TrezorMessage.LoadDevice.getDefaultInstance(), (short) 13),
  RESET_DEVICE(TrezorMessage.ResetDevice.getDefaultInstance(), (short) 14),

  // Signing
  SIGN_TX(TrezorMessage.SignTx.getDefaultInstance(), (short) 15),
  SIMPLE_SIGN_TX(TrezorMessage.SimpleSignTx.getDefaultInstance(), (short) 16),

  // Features
  FEATURES(TrezorMessage.Features.getDefaultInstance(), (short) 17),

  // PIN
  PIN_MATRIX_REQUEST(TrezorMessage.PinMatrixRequest.getDefaultInstance(), (short) 18),
  PIN_MATRIX_ACK(TrezorMessage.PinMatrixAck.getDefaultInstance(), (short) 19),
  CANCEL(TrezorMessage.Cancel.getDefaultInstance(), (short) 20),

  // Transactions
  TX_REQUEST(TrezorMessage.TxRequest.getDefaultInstance(), (short) 21),
  TX_INPUT(TrezorMessage.TxInput.getDefaultInstance(), (short) 23),
  TX_OUTPUT(TrezorMessage.TxOutput.getDefaultInstance(), (short) 24),
  APPLY_SETTINGS(TrezorMessage.ApplySettings.getDefaultInstance(), (short) 25),

  // Buttons
  BUTTON_REQUEST(TrezorMessage.ButtonRequest.getDefaultInstance(), (short) 26),
  BUTTON_ACK(TrezorMessage.ButtonAck.getDefaultInstance(), (short) 27),

  // Address
  GET_ADDRESS(TrezorMessage.GetAddress.getDefaultInstance(), (short) 29),
  ADDRESS(TrezorMessage.Address.getDefaultInstance(), (short) 30),

  // Entropy
  ENTROPY_REQUEST(TrezorMessage.GetAddress.getDefaultInstance(), (short) 35),
  ENTROPY_ACK(TrezorMessage.GetAddress.getDefaultInstance(), (short) 36),

  // Message signing
  SIGN_MESSAGE(TrezorMessage.GetAddress.getDefaultInstance(), (short) 38),
  VERIFY_MESSAGE(TrezorMessage.GetAddress.getDefaultInstance(), (short) 39),
  MESSAGE_SIGNATURE(TrezorMessage.GetAddress.getDefaultInstance(), (short) 40),

  // Passphrase
  PASSPHRASE_REQUEST(TrezorMessage.GetAddress.getDefaultInstance(), (short) 41),
  PASSPHRASE_ACK(TrezorMessage.GetAddress.getDefaultInstance(), (short) 42),

  // Transaction size
  ESTIMATE_TX_SIZE(TrezorMessage.GetAddress.getDefaultInstance(), (short) 43),
  TX_SIZE(TrezorMessage.GetAddress.getDefaultInstance(), (short) 44),

  // Debugging messages
  DEBUG_LINK_DECISION(TrezorMessage.DebugLinkDecision.getDefaultInstance(), (short) 100),
  DEBUG_LINK_GET_STATE(TrezorMessage.DebugLinkGetState.getDefaultInstance(), (short) 101),
  DEBUG_LINK_STATE(TrezorMessage.DebugLinkState.getDefaultInstance(), (short) 102),
  DEBUG_LINK_STOP(TrezorMessage.DebugLinkStop.getDefaultInstance(), (short) 103),
  DEBUG_LINK_LOG(TrezorMessage.DebugLinkLog.getDefaultInstance(), (short) 104),

  // End of enum
  ;

  private final Message message;
  private final short headerCode;

  /**
   * @param message    The protocol buffer message class
   * @param headerCode The header code
   */
  TrezorProtocolMessageType(Message message, short headerCode) {

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
  public static TrezorProtocolMessageType getMessageTypeByHeaderCode(short headerCode) {

    for (TrezorProtocolMessageType messageType : TrezorProtocolMessageType.values()) {

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

    for (TrezorProtocolMessageType messageType : TrezorProtocolMessageType.values()) {

      // Check for same type
      if (messageType.getDefaultInstance().getClass().equals(trezorMessage.getClass())) {
        return messageType.getHeaderCode();
      }
    }

    throw new IllegalArgumentException("Message class '" + trezorMessage.getClass().getName() + "' is not known");

  }

  public static Message parse(Short headerCode, byte[] bytes) throws InvalidProtocolBufferException {

    TrezorProtocolMessageType messageType = getMessageTypeByHeaderCode(headerCode);
    return messageType.getDefaultInstance().newBuilderForType().mergeFrom(bytes).build();

  }
}
