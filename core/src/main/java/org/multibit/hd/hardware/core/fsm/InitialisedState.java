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
 * <p>The "initialised" state is the awaiting state of a hardware wallet when the
 * features have been retrieved.</p>
 *
 * <p>The next state depends upon a user providing input based on the features.</p>
 *
 * @since 0.0.1
 *  
 */
public class InitialisedState extends AbstractHardwareWalletState {

  private static final Logger log = LoggerFactory.getLogger(InitialisedState.class);

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
