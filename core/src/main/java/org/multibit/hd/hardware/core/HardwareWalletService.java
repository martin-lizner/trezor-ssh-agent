package org.multibit.hd.hardware.core;

import com.google.common.eventbus.EventBus;

/**
 * <p>Service to provide the following to application:</p>
 * <ul>
 * <li>Main entry point to hardware wallet client selection</li>
 * <li>Handles hardware wallet event registration</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class HardwareWalletService {

  /**
   * The EventBus for distributing hardware wallet events
   */
  public static final EventBus hardwareEventBus = new EventBus();

  /**
   * <p>This method requires the Trezor JAR to be included</p>
   *
   * @return A dynamically bound production Trezor USB client
   */
  public static HardwareWalletClient newProductionTrezorUsbClient() {



    return null;
  }
}
