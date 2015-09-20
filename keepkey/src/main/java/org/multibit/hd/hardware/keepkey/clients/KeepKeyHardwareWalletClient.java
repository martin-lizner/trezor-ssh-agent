package org.multibit.hd.hardware.keepkey.clients;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.events.MessageEventType;
import org.multibit.hd.hardware.core.events.MessageEvents;
import org.multibit.hd.hardware.core.messages.Features;
import org.multibit.hd.hardware.keepkey.wallets.AbstractKeepKeyHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * <p>Client to provide the following to applications:</p>
 * <ul>
 * <li>Provides high level accessor methods based on common use cases</li>
 * </ul>
 * <p>This is intended as a high level API to the KeepKey device. Developers who need more control over the
 * responses and events are advised to study the Examples project and use the <code>KeepKeyHardwareWallet</code>
 * implementations.</p>
 *
 * @since 0.0.1
 *  
 */
public class KeepKeyHardwareWalletClient extends AbstractKeepKeyHardwareWalletClient {

  private static final Logger log = LoggerFactory.getLogger(KeepKeyHardwareWalletClient.class);

  private final AbstractKeepKeyHardwareWallet keepKey;
  private boolean isKeepKeyValid = false;

  /**
   * @param keepKey The KeepKey hardware wallet
   */
  public KeepKeyHardwareWalletClient(AbstractKeepKeyHardwareWallet keepKey) {

    Preconditions.checkNotNull(keepKey, "'keepKey' must be present");

    this.keepKey = keepKey;
  }

  @Override
  public String name() {
    return "KEEP_KEY";
  }

  @Override
  public boolean attach() {

    log.debug("Verifying environment...");

    if (!keepKey.attach()) {
      log.error("Problems with the hardware environment will prevent communication with the KeepKey.");
      return false;
    }

    // Must be OK to be here
    log.debug("Environment OK");

    return true;
  }

  @Override
  public void softDetach() {

    log.debug("Performing client soft detach...");

    isKeepKeyValid = false;

    keepKey.softDetach();
  }

  @Override
  public void hardDetach() {

    log.debug("Performing client hard detach...");

    isKeepKeyValid = false;

    // A hard detach includes a disconnect
    keepKey.hardDetach();

  }

  @Override
  public boolean connect() {

    log.debug("Attempting to connect...");
    isKeepKeyValid = keepKey.connect();

    if (isKeepKeyValid) {
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_CONNECTED, name());
    }

    return isKeepKeyValid;

  }

  @Override
  public void disconnect() {

    // A disconnect has the same behaviour as a soft detach
    softDetach();
  }

  @Override
  protected Optional<MessageEvent> sendMessage(Message message) {

    // Implemented as a blocking message
    return sendMessage(message, 1, TimeUnit.SECONDS);
  }

  @Override
  protected Optional<MessageEvent> sendMessage(Message message, int duration, TimeUnit timeUnit) {

    if (!isKeepKeyValid) {
      log.warn("KeepKey is not valid.");
      return Optional.absent();
    }

    // Write the message
    keepKey.writeMessage(message);

    return Optional.absent();

  }

  @Override
  public boolean verifyFeatures(Features features) {

    String version = features.getVersion();
    // Test for firmware compatibility
    if (version.startsWith("0.")) {
      log.warn("Unsupported firmware: {}", version);
      return false;
    }

    // Must be OK to be here
    return true;
  }

}
