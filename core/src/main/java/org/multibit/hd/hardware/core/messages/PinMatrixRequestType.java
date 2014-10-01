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
public enum PinMatrixRequestType {

  /**
   * Requesting the current PIN
   */
  CURRENT,

  /**
   * Requesting a new PIN for the first time
   */
  NEW_FIRST,

  /**
   * Requesting a new PIN for the second time to confirm
   */
  NEW_SECOND;

  // End of enum
  ;
}