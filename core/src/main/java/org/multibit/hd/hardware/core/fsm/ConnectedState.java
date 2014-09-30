package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.MessageEvent;

/**
 * <p>State to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>State transitions based on low level message events</li>
 * </ul>
 * <p>The "connected" state is the starting state of a hardware wallet when the
 * underlying communication transport confirms connection. In USB terms the device
 * is claimed, in a socket the server has accepted.</p>
 *
 * @since 0.0.1
 *  
 */
public class ConnectedState extends AbstractHardwareWalletState {

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

  }
}
