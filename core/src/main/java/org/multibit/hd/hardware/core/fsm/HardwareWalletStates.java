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
   * @return A new wiped state
   */
  public static WipedState newWipedState() {

    log.debug("Transitioning to 'wiped' state");

    return new WipedState();

  }

}
