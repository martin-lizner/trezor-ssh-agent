package org.multibit.hd.hardware.trezor.core;

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
public interface TrezorListener {

  /**
   * <p>Get the Trezor event queue (implementers should not create one)</p>
   */
  BlockingQueue<TrezorEvent> getTrezorEventQueue();

  /**
   * <p>Set the Trezor event queue (should be synchronized)</p>
   *
   * @param trezorEventQueue The blocking queue on which the events will be delivered
   */
  void setTrezorEventQueue(BlockingQueue<TrezorEvent> trezorEventQueue);

}
