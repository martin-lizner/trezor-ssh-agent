package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletEventType;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  /**
   * Non-static to allow for custom message from subclasses
   */
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Override
  public void await(HardwareWalletContext context) {
    // Do nothing
  }

  @Override
  public void transition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    // Handle low level message events for the device
    switch (event.getEventType()) {
      case DEVICE_ATTACHED:
        context.resetToAttached();
        return;
      case DEVICE_DETACHED:
        client.softDetach();
        context.resetToDetached();
        return;
      case DEVICE_CONNECTED:
        context.resetToConnected();
        return;
      case DEVICE_DISCONNECTED:
        context.resetToDisconnected();
        return;
      case DEVICE_FAILED:
        context.resetToFailed();
        return;
    }

    // Must be unhandled to be here so rely on internal handler
    // provided by implementation
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
   */
  protected abstract void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event);

  /**
   * <p>Provide standard handling for an unexpected message so the downstream consumer can react appropriately</p>
   *
   * @param context The current context providing parameters for decisions
   * @param event   The event driving the transition
   */
  protected void handleUnexpectedMessageEvent(HardwareWalletContext context, MessageEvent event) {

    log.warn("Unexpected message event '{}'", event.getEventType().name());
    if (event.getMessage().isPresent()) {
      HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_FAILED, event.getMessage().get());
    } else {
      HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_FAILED);
    }
    context.resetToConnected();

  }
}
