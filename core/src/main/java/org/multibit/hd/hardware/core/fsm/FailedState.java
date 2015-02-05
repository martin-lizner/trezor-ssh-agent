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
 * <p>The "failed" state represents when the underlying hardware has failed to perform
 * an operation. The device should not be used further without going through a connect
 * operation.</p>
 *
 * @since 0.0.1
 *  
 */
public class FailedState extends AbstractHardwareWalletState {

  private static final Logger log = LoggerFactory.getLogger(FailedState.class);

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      // TODO Implement
      default:
        handleUnexpectedMessageEvent(context, event);
    }

  }
}
