package org.multibit.hd.hardware.core.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Factory to provide the following to application API:</p>
 * <ul>
 * <li>Provision of all hardware wallet states</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class HardwareWalletStates {

  private static final Logger log = LoggerFactory.getLogger(HardwareWalletStates.class);

  /**
   * Utilities have a private constructor
   */
  private HardwareWalletStates() {
  }

  /**
   * @return A new stopped state
   */
  public static StoppedState newStoppedState() {

    log.debug("Transitioning to 'stopped' state");

    return new StoppedState();

  }

  /**
   * @return A new detached state
   */
  public static DetachedState newDetachedState() {

    log.debug("Transitioning to 'detached' state");

    return new DetachedState();

  }

  /**
   * @return A new attached state
   */
  public static AttachedState newAttachedState() {

    log.debug("Transitioning to 'attached' state");

    return new AttachedState();

  }

  /**
   * @return A new disconnected state
   */
  public static DisconnectedState newDisconnectedState() {

    log.debug("Transitioning to 'disconnected' state");

    return new DisconnectedState();

  }

  /**
   * @return A new connected state
   */
  public static ConnectedState newConnectedState() {

    log.debug("Transitioning to 'connected' state");

    return new ConnectedState();

  }

  /**
   * @return A new failed state
   */
  public static FailedState newFailedState() {

    log.debug("Transitioning to 'failed' state");

    return new FailedState();

  }

  /**
   * @return A new initialised state
   */
  public static InitialisedState newInitialisedState() {

    log.debug("Transitioning to 'initialised' state");

    return new InitialisedState();

  }

  /**
   * @return A new confirm wipe state
   */
  public static ConfirmWipeState newConfirmWipeState() {

    log.debug("Transitioning to 'confirm wipe' state");

    return new ConfirmWipeState();

  }

  /**
   * @return A new confirm reset state
   */
  public static ConfirmResetState newConfirmResetState() {

    log.debug("Transitioning to 'confirm reset' state");

    return new ConfirmResetState();

  }

  /**
   * @return A new confirm load state
   */
  public static ConfirmLoadState newConfirmLoadState() {

    log.debug("Transitioning to 'confirm load' state");

    return new ConfirmLoadState();

  }

  /**
   * @return A new confirm PIN state
   */
  public static ConfirmPINState newConfirmPINState() {

    log.debug("Transitioning to 'confirm PIN' state");

    return new ConfirmPINState();

  }

  /**
   * @return A new confirm entropy state
   */
  public static ConfirmEntropyState newConfirmEntropyState() {

    log.debug("Transitioning to 'confirm Entropy' state");

    return new ConfirmEntropyState();

  }

  /**
   * @return A new confirm get address state
   */
  public static ConfirmGetAddressState newConfirmGetAddressState() {

    log.debug("Transitioning to 'confirm GetAddress' state");

    return new ConfirmGetAddressState();

  }

  /**
   * @return A new confirm get public key state
   */
  public static ConfirmGetPublicKeyState newConfirmGetPublicKeyState() {

    log.debug("Transitioning to 'confirm GetPublicKey' state");

    return new ConfirmGetPublicKeyState();

  }

  /**
   * @return A new confirm get deterministic hierarchy state
   */
  public static ConfirmGetDeterministicHierarchyState newConfirmGetDeterministicHierarchyState() {

    log.debug("Transitioning to 'confirm GetDeterministicHierarchy' state");

    return new ConfirmGetDeterministicHierarchyState();

  }

  /**
   * @return A new confirm cipher key state
   */
  public static HardwareWalletState newConfirmCipherKeyState() {

    log.debug("Transitioning to 'confirm CipherKey' state");

    return new ConfirmCipherKeyState();

  }

  /**
   * @return A new confirm sign tx state
   */
  public static ConfirmSignTxState newConfirmSignTxState() {

    log.debug("Transitioning to 'confirm SignTx' state");

    return new ConfirmSignTxState();

  }

  /**
   * @return A new confirm sign message state
   */
  public static ConfirmSignMessageState newConfirmSignMessageState() {

    log.debug("Transitioning to 'confirm SignMessage' state");

    return new ConfirmSignMessageState();

  }

  /**
   * @return A new change PIN state
   */
  public static ConfirmChangePINState newConfirmChangePINState() {

    log.debug("Transitioning to 'confirm change PIN' state");

    return new ConfirmChangePINState();

  }

}
