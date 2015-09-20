package org.multibit.hd.hardware.core.fsm;

import com.google.common.collect.Lists;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.MainNetParams;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletEventType;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.messages.PublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <p>State to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>State transitions based on low level message events</li>
 * </ul>
 * <p>The "confirm get extended public key" state occurs in response to a GET_PUBLIC_KEY
 * message with reduced parameters and handles the ongoing button requests, success and
 * failure messages coming from the device as it provides the public key generated from
 * the seed phrase.</p>
 *
 * @since 0.0.1
 *  
 */
public class ConfirmGetDeterministicHierarchyState extends AbstractHardwareWalletState {

  private static final Logger log = LoggerFactory.getLogger(ConfirmGetDeterministicHierarchyState.class);

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      case PIN_MATRIX_REQUEST:
        // Device is asking for a PIN matrix to be displayed (user must read the screen carefully)
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_PIN_ENTRY, event.getMessage().get(), client.name());
        // Further state transitions will occur after the user has provided the PIN via the service
        break;
      case PASSPHRASE_REQUEST:
        // Device is asking for a passphrase screen to be displayed
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_PASSPHRASE_ENTRY, client.name());
        // Further state transitions will occur after the user has provided the passphrase via the service
        break;
      case PUBLIC_KEY:
        // Device has completed the operation and provided a public key
        PublicKey publicKey = (PublicKey) event.getMessage().get();

        // Gather some info
        int depth = publicKey.getHdNodeType().get().getDepth().get();
        String base58Xpub = publicKey.getXpub().get();
        List<ChildNumber> childNumbers = context.getChildNumbers().get();

        // Check current depth against the requirements
        if (depth <= childNumbers.size()) {

          // Update the current deterministic key forming the root of the hierarchy
          DeterministicKey parent = context.getDeterministicKey().orNull();
          log.debug("Parent key path: {}", parent == null ? "Root" : parent.getPathAsString());
          DeterministicKey child = DeterministicKey.deserializeB58(parent, base58Xpub, MainNetParams.get());
          log.debug("Child key path: {}", child.getPathAsString());
          context.setDeterministicKey(child);

        }

        if (depth == childNumbers.size()) {
          // We have reached the correct depth so we can create the hierarchy
          context.setDeterministicHierarchy(new DeterministicHierarchy(context.getDeterministicKey().get()));

          // Inform downstream consumers that we are ready
          // (deterministic hierarchy would require a wrapper for inclusion in the event itself)
          HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.DETERMINISTIC_HIERARCHY, client.name());
        }

        // Are further calls into the hierarchy required?
        if (depth < childNumbers.size()) {

          // Build up the child number list to include the next level
          int nextDepth = depth + 1;
          List<ChildNumber> nextChildNumbers = Lists.newArrayList();
          for (int i = 0; i < childNumbers.size(); i++) {
            if (i < nextDepth) {
              nextChildNumbers.add(childNumbers.get(i));
            }
          }
          client.getDeterministicHierarchy(nextChildNumbers);

        }

        break;
      case FAILURE:
        // User has cancelled or operation failed
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_FAILED, event.getMessage().get(), client.name());
        context.resetToInitialised();
        break;
      default:
        handleUnexpectedMessageEvent(context, event);
    }

  }
}
