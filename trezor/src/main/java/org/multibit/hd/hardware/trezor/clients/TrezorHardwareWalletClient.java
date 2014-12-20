package org.multibit.hd.hardware.trezor.clients;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.events.MessageEventType;
import org.multibit.hd.hardware.core.events.MessageEvents;
import org.multibit.hd.hardware.trezor.wallets.AbstractTrezorHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * <p>Client to provide the following to applications:</p>
 * <ul>
 * <li>Provides high level accessor methods based on common use cases</li>
 * </ul>
 * <p>This is intended as a high level API to the Trezor device. Developers who need more control over the
 * responses and events are advised to study the Examples project.</p>
 *
 * @since 0.0.1
 *  
 */
public class TrezorHardwareWalletClient extends AbstractTrezorHardwareWalletClient {

  private static final Logger log = LoggerFactory.getLogger(TrezorHardwareWalletClient.class);

  private final AbstractTrezorHardwareWallet trezor;
  private boolean isTrezorValid = false;

  /**
   * @param trezor The Trezor device
   */
  public TrezorHardwareWalletClient(AbstractTrezorHardwareWallet trezor) {
    this.trezor = trezor;
  }

  @Override
  public boolean attach() {

    log.debug("Verifying environment...");

    if (!trezor.attach()) {
      log.error("Problems with the hardware environment will prevent communication with the Trezor.");
      return false;
    }

    // Must be OK to be here
    log.debug("Environment OK");

    return true;
  }

  @Override
  public void softDetach() {

    log.debug("Detaching...");

    disconnect();
    trezor.softDetach();

  }

  @Override
  public void hardDetach() {

    log.debug("Hard detach...");

    disconnect();
    trezor.hardDetach();

  }

  @Override
  public boolean connect() {

    log.debug("Attempting to connect...");
    isTrezorValid = trezor.connect();

    if (isTrezorValid) {
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_CONNECTED);
    }

    return isTrezorValid;

  }

  @Override
  public void disconnect() {
    isTrezorValid = false;
    trezor.disconnect();
  }

  @Override
  protected Optional<MessageEvent> sendMessage(Message message) {

    // Implemented as a blocking message
    return sendMessage(message, 1, TimeUnit.SECONDS);
  }

  @Override
  protected Optional<MessageEvent> sendMessage(Message message, int duration, TimeUnit timeUnit) {

    Preconditions.checkState(isTrezorValid, "Trezor device is not valid. Try connecting or start a new session after a disconnect.");

    // Write the message
    trezor.writeMessage(message);

    return Optional.absent();

  }

}
