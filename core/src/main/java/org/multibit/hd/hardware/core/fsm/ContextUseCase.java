package org.multibit.hd.hardware.core.fsm;

/**
* <p>Enum to provide the following to FSM context:</p>
* <ul>
* <li>High level view of current use case</li>
* </ul>
*
* @since 0.0.1
*  
*/
public enum ContextUseCase {
  START,
  DETACHED,
  WIPE_DEVICE,
  LOAD_WALLET,
  PROVIDE_ENTROPY,
  CREATE_WALLET,
  REQUEST_ADDRESS,
  REQUEST_PUBLIC_KEY,
  REQUEST_DETERMINISTIC_HIERARCHY,
  SIMPLE_SIGN_TX,
  SIGN_TX,
  REQUEST_CIPHER_KEY,
  ENCRYPT_MESSAGE,
  DECRYPT_MESSAGE,
  SIGN_MESSAGE,
  VERIFY_MESSAGE,
  CHANGE_PIN

  // End of enum
  ;
}
