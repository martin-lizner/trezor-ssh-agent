package org.multibit.hd.hardware.trezor.clients;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.HardwareWalletMessageType;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

  private final HardwareWallet trezor;
  private boolean isTrezorValid = false;

  private ExecutorService trezorEventExecutorService = Executors.newSingleThreadExecutor();
  private boolean isSessionIdValid = true;

  /**
   * Keep track of hardware wallet events to allow blocking to occur
   */
  private final BlockingQueue<HardwareWalletEvent> hardwareWalletEvents = Queues.newArrayBlockingQueue(10);

  /**
   * @param trezor The Trezor device
   */
  public TrezorHardwareWalletClient(HardwareWallet trezor) {
    this.trezor = trezor;
  }

  @Override
  public boolean connect() {

    log.debug("Attempting to connect...");
    isTrezorValid = trezor.connect();
    return isTrezorValid;

  }

  @Override
  public void disconnect() {
    isSessionIdValid = false;
    isTrezorValid = false;
    trezor.disconnect();
    trezorEventExecutorService.shutdownNow();
  }

  @Override
  protected Optional<HardwareWalletEvent> sendMessage(Message message) {

    // Implemented as a blocking message
    return sendMessage(message, 1, TimeUnit.SECONDS);
  }

  @Override
  protected Optional<HardwareWalletEvent> sendMessage(Message message, int duration, TimeUnit timeUnit) {

    Preconditions.checkState(isTrezorValid, "Trezor device is not valid. Try connecting or start a new session after a disconnect.");
    Preconditions.checkState(isSessionIdValid, "An old session ID must be discarded. Create a new instance.");

    trezor.writeMessage(message);

    // Wait for a response
    try {
      return Optional.fromNullable(hardwareWalletEvents.poll(duration, timeUnit));
    } catch (InterruptedException e) {
      HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletMessageType.DEVICE_FAILED);
      return Optional.absent();
    }

  }

}
