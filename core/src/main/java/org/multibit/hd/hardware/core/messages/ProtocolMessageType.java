package org.multibit.hd.hardware.core.messages;

/**
 * <p>Enum to provide the following to application:</p>
 * <ul>
 * <li>Identification of protocol messages and their header codes</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public enum ProtocolMessageType {

  INITALIZE,

  PING,

  SUCCESS,
  FAILURE,

  GET_UUID,
  UUID,

  OTP_REQUEST,
  OTP_ACK,
  OTP_CANCEL,

  GET_ENTROPY,
  ENTROPY,

  GET_MASTER_PUBLIC_KEY,
  MASTER_PUBLIC_KEY,

  LOAD_DEVICE,
  RESET_DEVICE,

  SIGN_TX,
  FEATURES,

  PIN_REQUEST,
  PIN_ACK,
  PIN_CANCEL,

  // Transactions
  TX_REQUEST,
  TX_INPUT,
  TX_OUTPUT,
  SET_MAX_FEE_KB,

  // Buttons
  BUTTON_REQUEST,
  BUTTON_ACK,
  BUTTON_CANCEL,

  // Address
  GET_ADDRESS,
  ADDRESS,

  // Debugging messages
  DEBUG_LINK_DECISION,
  DEBUG_LINK_GET_STATE,
  DEBUG_LINK_STATE,
  DEBUG_LINK_STOP,

  // End of enum
  ;

}
