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

  // Connection
  INITALIZE,
  PING,

  // Generic responses
  SUCCESS,
  FAILURE,

  // Setup
  CHANGE_PIN,
  WIPE_DEVICE,
  FIRMWARE_ERASE,
  FIRMWARE_UPLOAD,

  // Entropy
  GET_ENTROPY,
  ENTROPY,

  // Key passing
  GET_PUBLIC_KEY,
  PUBLIC_KEY,

  // Load and reset
  LOAD_DEVICE,
  RESET_DEVICE,

  // Signing
  SIGN_TX,
  SIMPLE_SIGN_TX,
  FEATURES,

  // PIN
  PIN_MATRIX_REQUEST,
  PIN_MATRIX_ACK,
  CANCEL,

  // Transactions
  TX_REQUEST,
  TX_INPUT,
  TX_OUTPUT,
  APPLY_SETTINGS,

  // Buttons
  BUTTON_REQUEST,
  BUTTON_ACK,

  // Address
  GET_ADDRESS,
  ADDRESS,

  ENTROPY_REQUEST,
  ENTROPY_ACK,

  // Message signing
  SIGN_MESSAGE,
  VERIFY_MESSAGE,
  MESSAGE_SIGNATURE,

  // Passphrase
  PASSPHRASE_REQUEST,
  PASSPHRASE_ACK,

  // Transaction size
  ESTIMATE_TX_SIZE,
  TX_SIZE,

  // Debugging messages
  DEBUG_LINK_DECISION,
  DEBUG_LINK_GET_STATE,
  DEBUG_LINK_STATE,
  DEBUG_LINK_STOP,

  // End of enum
  ;

}
