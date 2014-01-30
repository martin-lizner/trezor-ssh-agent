package org.multibit.hd.hardware.core;

import com.google.common.eventbus.EventBus;

/**
 * <p>Interface to provide the following to applications:</p>
 * <ul>
 * <li>Common methods available to different hardware wallet devices</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public interface HardwareWalletClient {

  /**
   * @return The HardwareWalletSpecification in use for this exchange
   */
  HardwareWalletSpecification getSpecification();

  /**
   * @return A default HardwareWalletSpecification to use during the creation process if one is not supplied
   */
  HardwareWalletSpecification getDefaultSpecification();

  /**
   * Applies any exchange specific parameters
   *
   * @param specification The {@link HardwareWalletSpecification}
   */
  void applySpecification(HardwareWalletSpecification specification);

  /**
   * <p>Provide access to the hardware wallet event bus to register event handlers (see <a href="https://code.google.com/p/guava-libraries/wiki/EventBusExplained">Guava Event Bus</a>)</p>
   */
  EventBus getEventBus();

  /**
   * <p>Attempt a connection to the device</p>
   */
  void connect();

  /**
   * <p>Break the connection to the device</p>
   */
  void close();

}
