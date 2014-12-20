package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.MessageEvent;

/**
 * <p>State to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>State transitions based on low level message events</li>
 * </ul>
 * <p>The "attached" state represents when the underlying hardware has
 * detected a new device that matches the required parameters.</p>
 * <p>This could be a USB attachment or a server socket accepting a connection.</p>
 *
 * <p>The next state is normally Connected or Detached</p>
 *
 * @since 0.0.1
 *  
 */
public class AttachedState extends AbstractHardwareWalletState {

  @Override
  public void await(HardwareWalletContext context) {

    // Trigger a state transition via the response event
    context.getClient().connect();

  }

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

   context.resetToConnected();

  }
}
