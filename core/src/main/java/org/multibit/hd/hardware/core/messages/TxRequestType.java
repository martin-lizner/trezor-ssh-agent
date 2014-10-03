package org.multibit.hd.hardware.core.messages;

/**
 * <p>Value object to provide the following to high level messages:</p>
 * <ul>
 * <li>Transaction request type</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public enum TxRequestType {

  TX_INPUT,
  TX_OUTPUT,
  TX_META,
  TX_FINISHED,

  // End of enum
  ;

}