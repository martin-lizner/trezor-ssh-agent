package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.MessageEvent;

/**
 * <p>Interface to provide the following to hardware wallet finite state machines:</p>
 * <ul>
 * <li>Standard methods for managing state transitions from events</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public interface HardwareWalletState {

  /**
   * <p>Invoke the state's awaiting behaviour. This may cause an immediate state transition (such as
   * from Attached to Connected) or it may start a background process to wait for an event.</p>
   *
   * @param context The current context providing parameters for decisions
   */
  void await(HardwareWalletContext context);

  /**
   * <p>Initiate a move to the next state through the given client.</p>
   *
   * <p>Typically the client is used to move in to or out of a "waiting state" and the context is updated with new data</p>
   *
   * @param client  The hardware wallet client for sending messages
   * @param context The current context providing parameters for decisions
   * @param event   The event driving the transition
   */
  void transition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event);
}
