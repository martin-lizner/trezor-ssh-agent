package org.multibit.hd.hardware.core.device_transport;

import java.io.IOException;

/**
 *  <p>Interface to provide the following to devices (physical) hardware wallets:<br>
 *  <ul>
 *  <li>Transport of HIDInput and Output Streams to actual hardware wallets</li>
 *  </ul>
 *  </p>
 *  
 */
public interface DeviceTransport {
  /**
   * @return get the HIDInputStream that returns bytes from the physical device
   * @throws IOException
   */
  HIDInputStream getInputStream() throws IOException;

  /**
   * @return get the HIDOutputStream that sends bytes to the physical device
   * @throws IOException
   */
  HIDOutputStream getOutputStream() throws IOException;

  /**
   * Reset the device (as if it has just switched on)
   * @return The number of bytes sent in the feature report
   *
   * @throws java.io.IOException If something goes wrong
   */
  int reset() throws IOException;

}
