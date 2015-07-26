package org.multibit.hd.hardware.core.messages;

/**
 * <p>Enum to provide the following to high level messages:</p>
 * <ul>
 * <li>Language independent failure description</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public enum ButtonRequestType {

  /**
   * Catch-all for messages
   */
  OTHER,

  /**
   * Unexpectedly high fee
   */
  FEE_OVER_THRESHOLD,

  /**
   * Confirm an output
   */
  CONFIRM_OUTPUT,

  /**
   * Confirm reset device
   */
  RESET_DEVICE,

  /**
   * Confirm a word
   */
  CONFIRM_WORD,

  /**
   * Confirm wipe device
   */
  WIPE_DEVICE,

  /**
   * Protected call confirm action
   */
  PROTECT_CALL,

  /**
   * Confirm sign transaction
   */
  SIGN_TX,

  /**
   * Confirm firmware check
   */
  FIRMWARE_CHECK,

  /**
   * Confirm address generation
   */
  ADDRESS,
  ;

  // End of enum
  ;
}