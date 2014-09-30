package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.MessageEvent;

/**
 * <p>Abstract base class to provide the following to hardware wallet states:</p>
 * <ul>
 * <li>Access to common methods and fields</li>
 * </ul>
 *
 * <p></p>
 *
 * @since 0.0.1
 *  
 */
public abstract class AbstractHardwareWalletState implements HardwareWalletState {

  @Override
  public void transition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    // Handle standard message events
    switch (event.getMessageType()) {
      case DEVICE_ATTACHED:
        // Reset internal state to match the event
        context.resetToAttached();
        return;
      case DEVICE_CONNECTED:
        // Reset internal state to match the event
        context.resetToConnected();
        return;
      case DEVICE_DETACHED:
        // Reset internal state to match the event
        context.resetToDisconnected();
        return;
      case DEVICE_FAILED:
        // Reset internal state to match the event
        context.resetToFailed();
        return;
    }

    // Must be unhandled to be here so rely on specific handler
    internalTransition(client, context, event);

  }

  /**
   * <p>Initiate a move to the next state through the given client.</p>
   *
   * <p>Typically the client is used to move in to or out of a "waiting state" and the context is updated with new data</p>
   *
   * @param client  The hardware wallet client for sending messages
   * @param context The current context providing parameters for decisions
   * @param event   The event driving the transition
   *
   */
  protected abstract void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event);
}
