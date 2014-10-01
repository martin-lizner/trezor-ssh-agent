package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>State to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>State transitions based on low level message events</li>
 * </ul>
 * <p>The "wiped" state is the starting state of a hardware wallet when it contains no seed phrase.</p>
 *
 * @since 0.0.1
 *  
 */
public class WipedState extends AbstractHardwareWalletState {

  private static final Logger log = LoggerFactory.getLogger(WipedState.class);

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      // TODO Implement
      default:
        log.info("Unexpected message event '{}'", event.getEventType().name());
        context.resetToConnected();
    }

  }
}
