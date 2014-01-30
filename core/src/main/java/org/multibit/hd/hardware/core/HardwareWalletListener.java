package org.multibit.hd.hardware.core;

import org.multibit.hd.hardware.core.events.HardwareWalletEvent;

import java.util.concurrent.BlockingQueue;

/**
 * <p>Listener to provide the following to applications:</p>
 * <ul>
 * <li>Callback method to receive asynchronous events from the device</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public interface HardwareWalletListener {

  /**
   * <p>Get the HardwareWallet event queue (implementers should not create one)</p>
   */
  BlockingQueue<HardwareWalletEvent> getHardwareWalletEventQueue();

  /**
   * <p>Set the HardwareWallet event queue (should be synchronized)</p>
   *
   * @param HardwareWalletEventQueue The blocking queue on which the events will be delivered
   */
  void setHardwareWalletEventQueue(BlockingQueue<HardwareWalletEvent> HardwareWalletEventQueue);

}
