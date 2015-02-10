package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletEventType;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.messages.Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>State to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>State transitions based on low level message events</li>
 * </ul>
 * <p>The "connected" state is a transitional state occurring when the
 * underlying communication transport confirms connection. In USB terms the device
 * is claimed, in a socket the server has accepted.</p>
 *
 * <p>The next state is normally Initialised</p>
 *
 * @since 0.0.1
 *  
 */
public class ConnectedState extends AbstractHardwareWalletState {

  private static final Logger log = LoggerFactory.getLogger(ConnectedState.class);

  @Override
  public void await(HardwareWalletContext context) {

    // Trigger a state transition via the response event
    context.getClient().initialise();

  }

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      case FEATURES:
        Features features = (Features) event.getMessage().get();
        context.setFeatures(features);
        String version = features.getVersion();
        // Test for firmware compatibility
        if (version.startsWith("1.2.")
          || version.startsWith("1.1.")
          || version.startsWith("1.0.")
          || version.startsWith("0.")
          ) {
          log.warn("Unsupported firmware: {}", version);
          features.setSupported(false);
          context.resetToFailed();
        } else {
          features.setSupported(true);
          context.resetToInitialised();
        }
        break;
      case FAILURE:
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_FAILED, event.getMessage().get());
        context.resetToInitialised();
        break;
      default:
        handleUnexpectedMessageEvent(context, event);
    }

  }
}
