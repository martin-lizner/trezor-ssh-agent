package org.multibit.hd.hardware.core.events;

/**
 * <p>Enum to provide the following to application:</p>
 * <ul>
 * <li>Identification of protocol messages</li>
 * </ul>
 *
 * <p>These messages are considered to be common across all hardware wallets
 * supported through MultiBit Hardware.</p>
 *
 * @since 0.0.1
 *  
 */
public enum HardwareWalletMessageType {

  /**
   * Device encountered an error not associated with I/O (e.g. thread interrupt due to timeout)
   */
  DEVICE_FAILED,

  /**
   * Received EOF from device (no data in receive buffer after timeout when some is expected)
   */
  DEVICE_EOF,

  /**
   * Received on a device connect (communications established at the wire level)
   */
  DEVICE_CONNECTED,

  /**
   * Received on a device disconnect (no longer able to communicate)
   */
  DEVICE_DISCONNECTED,

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
  TX_ACK,

  // Cipher key
  CIPHER_KEY_VALUE,
  CLEAR_SESSION,

  // Settings
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

  // Message encryption
  ENCRYPT_MESSAGE,
  DECRYPT_MESSAGE,

  // Passphrase
  PASSPHRASE_REQUEST,
  PASSPHRASE_ACK,

  // Transaction size
  ESTIMATE_TX_SIZE,
  TX_SIZE,

  // Recovery
  RECOVER_DEVICE,

    // Word
  WORD_REQUEST,
  WORD_ACK,

  // Debugging messages
  DEBUG_LINK_DECISION,
  DEBUG_LINK_GET_STATE,
  DEBUG_LINK_STATE,
  DEBUG_LINK_STOP,
  DEBUG_LINK_LOG,

  // End of enum
  ;



}
