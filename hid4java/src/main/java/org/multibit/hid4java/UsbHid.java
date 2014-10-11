package org.multibit.hid4java;

import com.sun.jna.*;
import com.sun.jna.Structure.ByReference;

/**
 * <p>JNA bridge class to provide the following to USB HID:</p>
 * <ul>
 * <li>Access to the <code>signal11/hidapi</code> via JNA</li>
 * </ul>
 * <p>Requires the hidapi to be present on the classpath or the system library search path.</p>
 *
 * <p>Adapted from code found <a href="http://developer.mbed.org/cookbook/USBHID-bindings-">on the mbed.org site.</a></p>
 *
 * @since 0.0.1
 *  
 */
public class UsbHid {

  private static int WSTR_LEN = 512;
  private static final String DEVICE_NULL = "Device not initialised";
  private static final int DEVICE_ERROR = -2;

  private HidApiLibrary hidApiLibrary;

  public UsbHid() {
    hidApiLibrary = HidApiLibrary.INSTANCE;
  }

  /**
   * <p>Initialize the HIDAPI library.</p>
   * <p>This function initializes the HIDAPI library. Calling it is not strictly necessary,
   * as it will be called automatically by hid_enumerate() and any of the hid_open_*() functions
   * if it is needed. This function should be called at the beginning of execution however,
   * if there is a chance of HIDAPI handles being opened by different threads simultaneously.</p>
   */
  public void init() {
    hidApiLibrary.hid_init();
  }

  /**
   * <p>Finalize the HIDAPI library.</p>
   *
   * <p>This function frees all of the static data associated with HIDAPI. It should be called
   * at the end of execution to avoid memory leaks.</p>
   */
  public void exit() {
    hidApiLibrary.hid_exit();
  }

  /**
   * <p>Open a HID device using a Vendor ID (VID), Product ID (PID) and optionally a serial number.</p>
   */
  public HidDevice open(int vendor, int product, String serial_number) {

    Pointer p = hidApiLibrary.hid_open((short) vendor, (short) product, serial_number == null ? null : new WString(serial_number));
    return (p == null ? null : new HidDevice(p));

  }

  /**
   * <p>Open a HID device by its path name.</p>
   */
  public HidDevice open(String path) {
    Pointer p = hidApiLibrary.hid_open_path(path);
    return (p == null ? null : new HidDevice(p));
  }

  /**
   * <p>Close a HID device.</p>
   */
  public void close(HidDevice device) {

    if (device != null) {
      hidApiLibrary.hid_close(device.ptr());
    }

  }

  /**
   * <p>Get a string describing the last error which occurred.</p>
   */
  public String getLastErrorMessage(HidDevice device) {

    if (device == null) {
      return DEVICE_NULL;
    }

    Pointer p = hidApiLibrary.hid_error(device.ptr());

    return p == null ? null : new WideStringBuffer(p.getByteArray(0, WSTR_LEN)).toString();
  }

  /**
   * <p>Get The Manufacturer string from a HID device.</p>
   *
   * @param device The HID device
   */
  public String getManufacturer(HidDevice device) {

    if (device == null) {
      return DEVICE_NULL;
    }

    WideStringBuffer wStr = new WideStringBuffer(WSTR_LEN);
    int res = hidApiLibrary.hid_get_manufacturer_string(device.ptr(), wStr, WSTR_LEN);

    return wStr.toString();
  }

  /**
   * <p>Get The Product ID from a HID device.</p>
   *
   * @param device The HID device
   */
  public String getProductId(HidDevice device) {

    if (device == null) {
      return DEVICE_NULL;
    }

    WideStringBuffer wStr = new WideStringBuffer(WSTR_LEN);
    int res = hidApiLibrary.hid_get_product_string(device.ptr(), wStr, WSTR_LEN);

    return wStr.toString();
  }

  /**
   * <p>Get The Serial Number String from a HID device.</p>
   *
   * @param device The HID device
   */
  public String getSerialNumber(HidDevice device) {

    if (device == null) {
      return DEVICE_NULL;
    }

    WideStringBuffer wStr = new WideStringBuffer(WSTR_LEN);

    int res = hidApiLibrary.hid_get_serial_number_string(device.ptr(), wStr, WSTR_LEN);

    return wStr.toString();
  }

  /**
   * <p>Set the device handle to be non-blocking.</p>
   *
   * <p>In non-blocking mode calls to hid_read() will return immediately with a value of 0 if there is no data to be read.
   * In blocking mode, hid_read() will wait (block) until there is data to read before returning.</p>
   *
   * <p>Non-blocking can be turned on and off at any time.</p>
   *
   * @param device      The HID device
   * @param nonBlocking True if non-blocking mode is required
   */
  public boolean setNonBlocking(HidDevice device, boolean nonBlocking) {

    return device != null && 0 == hidApiLibrary.hid_set_nonblocking(device.ptr(), nonBlocking ? 1 : 0);

  }

  /**
   * <p>Read an Input report from a HID device.</p>
   * <p>Input reports are returned to the host through the INTERRUPT IN endpoint. The first byte
   * will contain the Report number if the device uses numbered reports.</p>
   *
   * @param device The HID device
   * @param bytes  The buffer to read into
   *
   * @return The actual number of bytes read and -1 on error. If no packet was available to be read
   * and the handle is in non-blocking mode, this function returns 0.
   */
  public int read(HidDevice device, byte[] bytes) {

    if (device == null || bytes == null) {
      return DEVICE_ERROR;
    }

    WideStringBuffer m = new WideStringBuffer(bytes);

    return hidApiLibrary.hid_read(device.ptr(), m, m.buffer.length);

  }

  /**
   * <p>Read an Input report from a HID device with timeout.</p>
   *
   * @param device        The HID device
   * @param bytes         The buffer to read into
   * @param timeoutMillis The number of milliseconds to wait before giving up
   *
   * @return The actual number of bytes read and -1 on error. If no packet was available to be read within
   * the timeout period returns 0.
   */
  public int read(HidDevice device, byte[] bytes, int timeoutMillis) {

    if (device == null || bytes == null) {
      return DEVICE_ERROR;
    }

    WideStringBuffer m = new WideStringBuffer(bytes);

    return hidApiLibrary.hid_read_timeout(device.ptr(), m, bytes.length, timeoutMillis);

  }

  /**
   * <p>Get a feature report from a HID device.</p>
   * <p>Under the covers the HID library will set the first byte of data[] to the Report ID of the report to be read.
   * Upon return, the first byte will still contain the Report ID, and the report data will start in data[1].</p>
   * <p>This method handles all the wide string and array manipulation for you.</p>
   *
   * @param device   The HID device
   * @param data     The buffer to contain the report
   * @param reportId The report ID (or (byte) 0x00)
   *
   * @return The number of bytes read plus one for the report ID (which has been removed from the first byte), or -1 on error.
   */
  public int getFeatureReport(HidDevice device, byte[] data, byte reportId) {

    if (device == null || data == null) {
      return DEVICE_ERROR;
    }

    // Create a large buffer
    WideStringBuffer m = new WideStringBuffer(WSTR_LEN);
    m.buffer[0] = reportId;
    int res = hidApiLibrary.hid_get_feature_report(device.ptr(), m, data.length + 1);

    if (res == -1) {
      return res;
    }

    System.arraycopy(m.buffer, 1, data, 0, res);
    return res;

  }

  /**
   * <p>Send a Feature report to the device.</p>
   *
   * <p>Under the covers, feature reports are sent over the Control endpoint as a Set_Report transfer.
   * The first byte of data[] must contain the Report ID. For devices which only support a single report,
   * this must be set to 0x0. The remaining bytes contain the report data.</p>
   * <p>Since the Report ID is mandatory, calls to hid_send_feature_report() will always contain one more byte than
   * the report contains. For example, if a hid report is 16 bytes long, 17 bytes must be passed to
   * hid_send_feature_report(): the Report ID (or 0x0, for devices which do not use numbered reports), followed by
   * the report data (16 bytes). In this example, the length passed in would be 17.</p>
   *
   * <p>This method handles all the array manipulation for you.</p>
   *
   * @param device   The HID device
   * @param data     The feature report data (will be widened and have the report ID pre-pended)
   * @param reportId The report ID (or (byte) 0x00)
   *
   * @return This function returns the actual number of bytes written and -1 on error.
   */
  public int sendFeatureReport(HidDevice device, byte[] data, byte reportId) {

    if (device == null || data == null) {
      return DEVICE_ERROR;
    }

    WideStringBuffer m = new WideStringBuffer(data.length + 1);
    m.buffer[0] = reportId;

    System.arraycopy(data, 0, m.buffer, 1, data.length);
    return hidApiLibrary.hid_send_feature_report(device.ptr(), m, m.buffer.length);

  }

  /**
   * <p>Get a string from a HID device, based on its string index</p>
   *
   * @param device The HID device
   * @param idx    The index
   *
   * @return The string
   */
  public String getIndexedString(HidDevice device, int idx) {

    if (device == null) {
      return DEVICE_NULL;
    }
    WideStringBuffer wStr = new WideStringBuffer(WSTR_LEN);
    int res = hidApiLibrary.hid_get_indexed_string(device.ptr(), idx, wStr, WSTR_LEN);

    return res == -1 ? null : wStr.toString();
  }

  /**
   * <p>Write an Output report to a HID device</p>
   *
   * <p>The first byte of data[] must contain the Report ID. For devices which only support a single report, this must be set to 0x0.
   * The remaining bytes contain the report data. Since the Report ID is mandatory, calls to hid_write() will always contain one more
   * byte than the report contains. For example, if a hid report is 16 bytes long, 17 bytes must be passed to hid_write(), the Report ID
   * (or 0x0, for devices with a single report), followed by the report data (16 bytes). In this example, the length passed in would
   * be 17.</p>
   *
   * <p>hid_write() will send the data on the first OUT endpoint, if one exists. If it does not, it will send the data through the
   * Control Endpoint (Endpoint 0).</p>
   *
   * @param device   The device
   * @param bytes    The bytes to write
   * @param len      The length
   * @param reportId The report ID (or (byte) 0x00)
   *
   * @return The number of bytes written, or -1 if an error occurs
   */
  public int write(HidDevice device, byte[] bytes, int len, byte reportId) {

    if (device == null || bytes == null) {
      return DEVICE_ERROR;
    }

    WideStringBuffer m = new WideStringBuffer(len + 1);
    m.buffer[0] = reportId;

    if (bytes.length < len) {
      len = bytes.length;
    }

    if (len > 1) {
      System.arraycopy(bytes, 0, m.buffer, 1, len);
    }

    return hidApiLibrary.hid_write(device.ptr(), m, m.buffer.length);
  }

  /**
   * <p>Enumerate the attached HID devices</p>
   *
   * @param vendor  The vendor ID
   * @param product The product ID
   *
   * @return The device info of the matching device
   */
  public HidDeviceInfo enumerateDevices(int vendor, int product) {

    return hidApiLibrary.hid_enumerate((short) vendor, (short) product);

  }

  /**
   * <p>Free an enumeration linked list</p>
   *
   * @param list The list to free
   */
  public void freeEnumeration(HidDeviceInfo list) {

    hidApiLibrary.hid_free_enumeration(list.getPointer());

  }

  /**
   * <p>Wrapper for a wide character (WCHAR) structure</p>
   */
  public static class WideStringBuffer extends Structure implements ByReference {

    public byte[] buffer = null;

    WideStringBuffer(int len) {
      buffer = new byte[len];
    }

    WideStringBuffer(byte[] bytes) {
      buffer = bytes;
    }

    /**
     * <p>hidapi uses wchar_t which is written l i k e   t h i s (with '\0' in between)</p>
     */
    public String toString() {
      String str = "";
      for (int i = 0; i < buffer.length && buffer[i] != 0; i += 2)
        str += (char) (buffer[i] | buffer[i + 1] << 8);
      return str;
    }

  }

  /**
   * <p>Value object to provide a HID device pointer</p>
   */
  public static class HidDevice extends Structure implements ByReference {

    public Pointer ptr;

    public HidDevice(Pointer p) {
      ptr = p;
    }

    public Pointer ptr() {
      return ptr;
    }
  }

  /**
   * <p>Value object to provide HID device information</p>
   */
  public static class HidDeviceInfo extends Structure implements ByReference {

    /**
     * USB path
     */
    public String path;

    /**
     * Vendor ID
     */
    public short vendor_id;
    /**
     * Produce ID
     */
    public short product_id;
    /**
     * Serial number
     */
    public WString serial_number;

    /**
     * Release number
     */
    public short release_number;
    /**
     * Manufacturer string
     */
    public WString manufacturer_string;

    /**
     * Usage Page for this Device/Interface (Windows/Mac only)
     */
    public WString product_string;
    /**
     * Usage for this Device/Interface (Windows/Mac only)
     */
    public short usage_page;

    /**
     * Usage number
     */
    public short usage;
    /**
     * Interface number
     */
    public int interface_number;

    /**
     * Reference to next device
     */
    // Consider public HidDeviceInfo.ByReference next;
    public HidDeviceInfo next;

    public HidDeviceInfo next() {
      return next;
    }

    public boolean hasNext() {
      return next != null;
    }

    /**
     * @return A string representation of the attached device
     */
    public String show() {
      HidDeviceInfo u = this;
      String str = "HidDeviceInfo\n";
      str += "\tpath:" + u.path + ">\n";
      str += "\tvendor_id: " + Integer.toHexString(u.vendor_id) + "\n";
      str += "\tproduct_id: " + Integer.toHexString(u.product_id) + "\n";
      str += "\tserial_number: " + u.serial_number + ">\n";
      str += "\trelease_number: " + u.release_number + "\n";
      str += "\tmanufacturer_string: " + u.manufacturer_string + ">\n";
      str += "\tproduct_string: " + u.product_string + ">\n";
      str += "\tusage_page: " + u.usage_page + "\n";
      str += "\tusage: " + u.usage + "\n";
      str += "\tinterface_number: " + u.interface_number + "\n";
      return str;
    }
  }

  /**
   * <p>JNA library interface to act as the proxy for the underlying native library</p>
   * <p>This approach removes the need for any JNI or native code</p>
   */
  public interface HidApiLibrary extends Library {

    HidApiLibrary INSTANCE = (HidApiLibrary) Native.loadLibrary("hidapi", HidApiLibrary.class);

    void hid_init();

    void hid_exit();

    Pointer hid_open(short vendor_id, short product_id, WString serial_number);

    void hid_close(Pointer device);

    Pointer hid_error(Pointer device);

    int hid_read(Pointer device, WideStringBuffer.ByReference bytes, int length);

    int hid_read_timeout(Pointer device, WideStringBuffer.ByReference bytes, int length, int timeout);

    int hid_write(Pointer device, WideStringBuffer.ByReference data, int len);

    int hid_get_feature_report(Pointer device, WideStringBuffer.ByReference data, int length);

    int hid_send_feature_report(Pointer device, WideStringBuffer.ByReference data, int length);

    int hid_get_indexed_string(Pointer device, int idx, WideStringBuffer.ByReference string, int len);

    int hid_get_manufacturer_string(Pointer device, WideStringBuffer.ByReference str, int len);

    int hid_get_product_string(Pointer device, WideStringBuffer.ByReference str, int len);

    int hid_get_serial_number_string(Pointer device, WideStringBuffer.ByReference str, int len);

    int hid_set_nonblocking(Pointer device, int nonblock);

    HidDeviceInfo hid_enumerate(short vendor_id, short product_id);

    void hid_free_enumeration(Pointer devs);

    Pointer hid_open_path(String path);
  }
}
