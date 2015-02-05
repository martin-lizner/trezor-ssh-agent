package org.multibit.hd.hardware.core.events;

/**
 * <p>Enum to provide the following to application:</p>
 * <ul>
 * <li>Identification of low level messages</li>
 * </ul>
 *
 * <p>These low level messages are considered to be common
 * across all hardware wallets supported through MultiBit Hardware.</p>
 *
 * @since 0.0.1
 *  
 */
public enum MessageEventType {

  // Device connectivity and communication
  /**
   * Notification. Received when a device encountered an error in the environment (e.g. timeout or native library load failure)
   */
  DEVICE_FAILED,
  /**
   * Notification. Received when a device is attached (device present but no communications yet attempted at the wire level)
   */
  DEVICE_ATTACHED,
  /**
   * Notification. Received when a device is detached (no device present)
   */
  DEVICE_DETACHED,
  /**
   * Notification. Received on a device connect (device present and communications established at the wire level)
   */
  DEVICE_CONNECTED,
  /**
   * Notification. Received on a device disconnect (device present and communications were established at the wire level but they are now blocked)
   */
  DEVICE_DISCONNECTED,
  /**
   * Notification. Received when a device is detached permanently (no device present, native libraries shut down)
   */
  DEVICE_DETACHED_HARD,

  // Connection
  /**
   * Client request. Initialise the device.
   */
  INITALISE,
  /**
   * Device response. Provides features supported by the device.
   */
  FEATURES,
  /**
   * Client request. Verify device is present.
   */
  PING,

  // Generic responses
  /**
   * Device response. Indicates the operation was a success if no specific message is defined.
   */
  SUCCESS,
  /**
   * Device response. Indicates the operation was a failure if no specific message is defined.
   */
  FAILURE,
  /**
   * Client request. Device should cancel current operation and return to initialised state.
   */
  CANCEL,
  /**
   * Client request. Device should clear its current session and return to initialised state.
   */
  CLEAR_SESSION,

  // Setup
  /**
   * Client request. Start the PIN change use case.
   */
  CHANGE_PIN,
  /**
   * Client request. Wipe the device back to factory defaults.
   */
  WIPE_DEVICE,
  /**
   * Client request. Load the device with client provided configuration and seed phrase.
   */
  LOAD_DEVICE,
  /**
   * Client request. Reset the device using client provided configuration and device provided seed phrase (recommended).
   */
  RESET_DEVICE,
  /**
   * Device request. Client should generate entropy from a secure random source.
   */
  ENTROPY_REQUEST,
  /**
   * Client response.
   */
  ENTROPY_ACK,


  // Firmware
  /**
   * Client request. Erase the current firmware.
   */
  FIRMWARE_ERASE,
  /**
   * Client request. Upload new firmware.
   */
  FIRMWARE_UPLOAD,

  // Entropy
  /**
   * Device request. Client should provide additional entropy to device.
   */
  GET_ENTROPY,
  /**
   * Client response. Provide additional entropy.
   */
  ENTROPY,

  // Key passing
  /**
   * Client request. Get the public key for the device root node (master public key).
   */
  GET_PUBLIC_KEY,
  /**
   * Device response. Provide the master public key.
   */
  PUBLIC_KEY,

  // Transaction signing
  /**
   * Client request. Start the transaction signing process (unlimited inputs/outputs).
   */
  SIGN_TX,
  /**
   * Client request. Start the simple transaction signing process (limited inputs/outputs)
   */
  SIMPLE_SIGN_TX,
  /**
   * Device request. Client should provide transaction input/output as required.
   */
  TX_REQUEST,
  /**
   * Client response. Client provides transaction input/output to device.
   */
  TX_ACK,

  // PIN
  /**
   * Device request. Client should show user PIN matrix and receive user input.
   */
  PIN_MATRIX_REQUEST,
  /**
   * Client response. Client provides obfuscated PIN as entered on matrix.
   */
  PIN_MATRIX_ACK,

  // Cipher key
  /**
   * Client request. Client requires symmetric en/decryption (deterministic) of payload
   */
  CIPHER_KEY_VALUE,
  /**
   * Device response. Device provides payload.
   */
  CIPHERED_KEY_VALUE,

  // Settings
  /**
   * Client request. Apply the settings to the device.
   */
  APPLY_SETTINGS,

  // Buttons
  /**
   * Device request. Client should show user a message indicating a button press on the device is required.
   */
  BUTTON_REQUEST,
  /**
   * Client response. Client provides acknowledgement that the user message is being shown.
   */
  BUTTON_ACK,

  // Address
  /**
   * Client request. Get a specified address given suitable co-ordinates.
   */
  GET_ADDRESS,
  /**
   * Device response. Provide the address at the co-ordinates.
   */
  ADDRESS,

  // Message signing
  /**
   * Client request. Device should sign message using given co-ordinates.
   */
  SIGN_MESSAGE,
  /**
   * Device response. Provide the signature for the message.
   */
  MESSAGE_SIGNATURE,
  /**
   * Client request. Device should verify message using given co-ordinates.
   */
  VERIFY_MESSAGE,

  // Message encryption
  /**
   * Client request. Device should encrypt message using given parameters.
   */
  ENCRYPT_MESSAGE,
  /**
   * Device response. Device provides payload.
   */
  ENCRYPTED_MESSAGE,
  /**
   * Client request. Device should decrypt message using given parameters.
   */
  DECRYPT_MESSAGE,
  /**
   * Device response. Device provides payload.
   */
  DECRYPTED_MESSAGE,

  // Passphrase
  /**
   * Device request. Client should ask user for passphrase.
   */
  PASSPHRASE_REQUEST,
  /**
   * Client response. Client provides passphrase from user.
   */
  PASSPHRASE_ACK,

  // Transaction size
  /**
   * Client request. Device should estimate the size in bytes of the provided transaction.
   */
  ESTIMATE_TX_SIZE,
  /**
   * Device response. The estimated size of the provided transaction.
   */
  TX_SIZE,

  // Recovery
  /**
   * Client request. Device should begin the process of seed phrase recovery.
   */
  RECOVER_DEVICE,
  /**
   * Device request. Client should ask user for a particular word from the seed phrase.
   */
  WORD_REQUEST,
  /**
   * Client response. Client provides seed phrase word from user.
   */
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
