package org.multibit.hd.hardware.core.wallets;

/**
 * <p>Utility interface to provide the following to hardware wallet clients and devices:</p>
 * <ul>
 * <li>Provision of standard lifecycle entry points</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public interface Connectable {

  /**
   * <p>Handle device attachment. The transport to the device is new formed (device attached, socket server started etc)</p>
   * <p>Implementations should verify the supporting environment before attempting a connection. Typically this would involve
   * initialising native libraries and verifying their communications</p>
   *
   * @return True if the native libraries initialised successfully
   */
  boolean attach();

  /**
   * <p>Handle device detachment. The transport to the device is gone (device removed, socket server shut down etc)</p>
   * <p>Implementations may assume that recovery is possible (the hardware drivers still remain operational)</p>
   */
  void softDetach();

  /**
   * <p>Handle device detachment and imminent shutdown.</p>
   * <p>Recovery is not possible (the controlling thread is about to close, hardware drivers are closing)</p>
   */
  void hardDetach();

  /**
   * <p>Attempt a connection to the underlying hardware to establish communication only (no higher level messages)</p>
   *
   * <p>Implementers must ensure the following behaviour:</p>
   * <ul>
   * <li>The hardware supporting the device is assumed to be physically attached and discoverable (i.e. plugged into USB or network)</li>
   * <li>Method will return false if no matching device is found and further queries are safe to repeat (polling is permitted)</li>
   * <li>A DEVICE_FAILED event will be generated if subsequent communication fails (i.e. USB HID fails or device is broken)</li>
   * </ul>
   *
   * @return True if the connection was successful
   */
  boolean connect();

  /**
   * <p>Break the connection to the device</p>
   *
   * <p>Implementers must ensure the following behaviour:</p>
   * <ul>
   * <li>All non-persistent state associated with the device is reset</li>
   * <li>A connect is required to restart communications</li>
   * </ul>
   */
  void disconnect();

}
