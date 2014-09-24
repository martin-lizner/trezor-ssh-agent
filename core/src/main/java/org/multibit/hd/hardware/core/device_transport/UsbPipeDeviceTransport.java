package org.multibit.hd.hardware.core.device_transport;

import com.codeminders.hidapi.HIDDevice;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>USB pipe HID wrapper to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>Low level read/write operations with a USB pipe through libusb</li>
 * <li>64-byte HID framing</li>
 * </ul>
 *
 * <p>This supports a version 1 production Trezor connected directly to the machine
 * through USB and is the default method of communicating on Winodws and Linux machines.</p>
 */
public class UsbPipeDeviceTransport implements DeviceTransport {

  /**
   * Provides logging for this class
   */
  private static final Logger log = LoggerFactory.getLogger(UsbPipeDeviceTransport.class);

  private final HIDDevice device;
  private final HIDInputStream hidInputStream;
  private final HIDOutputStream hidOutputStream;

  /**
   * @param device The HID device providing the low-level communications
   *
   * @throws java.io.IOException If something goes wrong
   */
  public UsbPipeDeviceTransport(HIDDevice device) throws IOException {
    this.device = device;
    this.hidInputStream = new HIDInputStream(device);
    this.hidOutputStream = new HIDOutputStream(device);
  }

  /**
   * <p>Provides an input stream consisting of incoming HID message payload data (no length byte)</p>
   * <p>Applications will use this to read data from the device, perhaps into a Protcol Buffer parser</p>
   *
   * @return The HID input stream
   *
   * @throws java.io.IOException If something goes wrong
   */
  public HIDInputStream getInputStream() throws IOException {
    return this.hidInputStream;
  }

  /**
   * <p>Provides an output stream consisting of outgoing HID message payload data (no length byte)</p>
   * <p>Applications will use this to write data to the device, perhaps from a Protcol Buffer serializer</p>
   *
   * @return The HID input stream
   *
   * @throws java.io.IOException If something goes wrong
   */
  public HIDOutputStream getOutputStream() throws IOException {
    return this.hidOutputStream;

  }

  /**
   * <p>Reset the UART (equivalent to unplug then replug without warning)
   * </p>
   *
   * @return The number of bytes sent in the feature report
   *
   * @throws java.io.IOException If something goes wrong
   */
  public int reset() throws IOException {

    Preconditions.checkNotNull(device, "Device is not connected");

    byte[] featureReport;
    featureReport = new byte[]{0x040, 0x00};

    int bytesSent = device.sendFeatureReport(featureReport);
    log.debug("> UART Reset: {} '{}'", bytesSent, featureReport);

    return bytesSent;

  }

}
