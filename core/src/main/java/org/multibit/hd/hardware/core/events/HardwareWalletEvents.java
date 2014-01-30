package org.multibit.hd.hardware.core.events;

import com.google.common.base.Preconditions;
import com.google.protobuf.Message;

/**
 * <p>Factory to provide the following to event producers:</p>
 * <ul>
 * <li>New instances of standard HardwareWallet events</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public class HardwareWalletEvents {

  /**
   * <p>A protocol event is one that falls within the HardwareWallet communications protocol (i.e. not a DISCONNECTED or similar)</p>
   *
   * @param messageType The message type (e.g. SUCCESS)
   * @param message     The message itself (from protocol buffers)
   *
   * @return A new immutable event instance
   */
  public static HardwareWalletEvents newProtocolEvent(final HardwareWalletEventType messageType, final Message message) {

    return null;

  }

  /**
   * <p>A system event is one that falls outside of the HardwareWallet communications protocol (i.e. a DISCONNECTED or similar)</p>
   *
   * @param HardwareWalletEventType The event type (e.g. DISCONNECTED)
   * @return A new immutable event instance
   */
  public static HardwareWalletEvents newSystemEvent(final HardwareWalletEventType HardwareWalletEventType) {

    Preconditions.checkNotNull(HardwareWalletEventType);

    return null;

  }


}
