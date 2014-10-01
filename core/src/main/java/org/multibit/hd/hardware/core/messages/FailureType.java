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
public enum FailureType {

  /**
   *
   */
  UNEXPECTED_MESSAGE,

  /**
   *
   */
  BUTTON_EXPECTED,

  /**
   *
   */
  SYNTAX_ERROR,

  /**
   *
   */
  ACTION_CANCELLED,

  /**
   *
   */
  PIN_EXPECTED,

  /**
   *
   */
  PIN_CANCELLED,

  /**
   *
   */
  PIN_INVALID,

  /**
   *
   */
  INVALID_SIGNATURE,

  /**
   *
   */
  OTHER,

  /**
   *
   */
  NOT_ENOUGH_FUNDS,

  /**
   *
   */
  NOT_INITIALIZED,

  /**
   *
   */
  FIRMWARE_ERROR,

  // End of enum
  ;
}