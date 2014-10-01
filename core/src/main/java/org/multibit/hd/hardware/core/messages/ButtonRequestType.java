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
   * 
   */
  OTHER,

  /**
   * 
   */
  FEE_OVER_THRESHOLD,

  /**
   * 
   */
  CONFIRM_OUTPUT,

  /**
   * 
   */
  RESET_DEVICE,

  /**
   * 
   */
  CONFIRM_WORD,

  /**
   * 
   */
  WIPE_DEVICE,

  /**
   *
   */
  PROTECT_CALL,

  /**
   * 
   */
  SIGN_TX,

  /**
   *
   */
  FIRMWARE_CHECK,

  /**
   *
   */
  ADDRESS,
  ;

  // End of enum
  ;
}