package org.multibit.hd.hardware.trezor.wallets.shield;

import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.HardwareWalletMessageType;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.trezor.utils.TrezorMessageUtils;
import org.multibit.hd.hardware.trezor.wallets.AbstractTrezorHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

/**
 * <p>Trezor implementation to provide the following to applications:</p>
 * <ul>
 * <li>Access to the Trezor RPi emulation device over USB</li>
 * </ul>
 *
 * <p>This class uses <code>hidapi</code> for each platform due to the
 * custom UART-to-USB present on the RPi Shield hardware.</p>
 *
 * @since 0.0.1
 *  
 */
public class TrezorShieldUsbHardwareWallet extends AbstractTrezorHardwareWallet {

  // Use the Raspberry Pi UART-to-USB bridge identifiers
  private static final Integer RPI_UART_VENDOR_ID = 0x10c4;
  private static final Integer RPI_UART_PRODUCT_ID = 0xea80;

  private static final Logger log = LoggerFactory.getLogger(TrezorShieldUsbHardwareWallet.class);

  private Optional<Short> vendorId = Optional.absent();
  private Optional<Short> productId = Optional.absent();
  private Optional<String> serialNumber = Optional.absent();

  private Optional<HIDDevice> deviceOptional = Optional.absent();

  static {
    Locale.setDefault(Locale.UK);
  }

  /**
   * Default constructor for use with dynamic binding
   */
  public TrezorShieldUsbHardwareWallet() {
    this(Optional.<Short>absent(), Optional.<Short>absent(), Optional.<String>absent());

  }

  /**
   * <p>Create a new instance of a USB-based Trezor emulator running on a Raspberry Pi with the Shield hardware</p>
   *
   * @param vendorId     The vendor ID (default is 0x10c4)
   * @param productId    The product ID (default is 0xea80)
   * @param serialNumber The device serial number (default is to accept any)
   */
  public TrezorShieldUsbHardwareWallet(Optional<Short> vendorId,
                                       Optional<Short> productId,
                                       Optional<String> serialNumber) {

    // Initialise the HID library
    if (!ClassPathLibraryLoader.loadNativeHIDLibrary()) {
      throw new IllegalStateException(
        "Unable to load native USB library. Check class loader permissions/JAR integrity.");
    }

    this.vendorId = vendorId;
    this.productId = productId;
    this.serialNumber = serialNumber;

    verifyEnvironment();

  }

  @Override
  public void applySpecification(HardwareWalletSpecification specification) {

    super.applySpecification(specification);

    // Specification overrides constructor if present
    this.vendorId = specification.getVendorId().isPresent() ? specification.getVendorId() : this.vendorId;
    this.productId = specification.getProductId().isPresent() ? specification.getProductId() : this.productId;
    this.serialNumber = specification.getSerialNumber().isPresent() ? specification.getSerialNumber() : this.serialNumber;

  }

  @Override
  public boolean verifyEnvironment() {

    try {
      deviceOptional = locateDevice();
    } catch (IOException e) {
      log.error("Failed to connect device due to USB problem", e);
      return false;
    }

    if (!deviceOptional.isPresent()) {
      log.info("Failed to connect device due to unmatched device (assumed disconnected)");
      return false;
    }

    // Must be OK to be here
    return true;

  }

  @Override
  public synchronized boolean connect() {

    // Expect a present device to be here

    HIDDevice device = deviceOptional.get();

    try {
      log.debug("Selected: {}, {}, {}",
        device.getManufacturerString(),
        device.getProductString(),
        device.getSerialNumberString()
      );
    } catch (IOException e) {
      log.error("Failed to connect device due to problem reading device information", e);
      return false;
    }

    // Attempt to open UART communications
    try {
      return attachDevice(device);
    } catch (IOException e) {
      log.error("Failed to attach device due to problem reading UART data stream", e);
      HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletMessageType.DEVICE_FAILED);
    }

    // Must have failed to be here
    return false;

  }

  /**
   * @param device The HID device to communicate with
   *
   * @return True if the device responded to the UART serial initialisation
   *
   * @throws java.io.IOException If something goes wrong
   */
  private boolean attachDevice(HIDDevice device) throws IOException {

    byte[] featureReport;

    // Create and configure the USB to UART bridge

    // Reset the device
    featureReport = new byte[]{0x040, 0x00};
    int bytesSent = device.sendFeatureReport(featureReport);
    log.debug("> UART Reset: {} '{}'", bytesSent, featureReport);

    // Enable the UART
    featureReport = new byte[]{0x041, 0x01};
    bytesSent = device.sendFeatureReport(featureReport);
    log.debug("> UART Enable: {} '{}'", bytesSent, featureReport);

    // Purge Rx and Tx buffers
    featureReport = new byte[]{0x043, (byte) 0x03};
    bytesSent = device.sendFeatureReport(featureReport);
    log.debug("> Purge RxTx: {} '{}'", bytesSent, featureReport);

    // Must have connected to be here
    HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletMessageType.DEVICE_CONNECTED);

    return true;

  }

  /**
   * @return A HID device with the given vendor, product and serial number IDs
   *
   * @throws java.io.IOException If something goes wrong with the USB
   */
  private Optional<HIDDevice> locateDevice() throws IOException {

    Preconditions.checkState(isDeviceConnected(), "Device is already connected");

    // Attempt to locate an attached Trezor device
    final Optional<HIDDeviceInfo> hidDeviceInfoOptional = locateTrezor();

    // No matching device so indicate that it is disconnected
    if (!hidDeviceInfoOptional.isPresent()) {
      HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletMessageType.DEVICE_DETACHED);
      return Optional.absent();
    }

    HIDDeviceInfo hidDeviceInfo = hidDeviceInfoOptional.get();

    // Get the HID manager
    HIDManager hidManager = HIDManager.getInstance();

    // Attempt to open a serial connection to the USB device
    HIDDevice hidDevice = hidManager.openById(
      hidDeviceInfo.getVendor_id(),
      hidDeviceInfo.getProduct_id(),
      null
    );

    if (hidDevice != null) {
      log.info("Successfully opened device.");
    } else {
      log.warn("Failed to open device.");
    }

    return Optional.fromNullable(hidDevice);

  }

  /**
   * @return True if device is connected (a HID device is present)
   */
  private boolean isDeviceConnected() {
    return deviceOptional != null;
  }

  @Override
  public synchronized void internalClose() {

    Preconditions.checkState(isDeviceConnected(), "Device is not connected");

    // Attempt to close the connection (also closes the in/out streams)
    try {
      deviceOptional.get().close();

      log.info("Disconnected from Trezor");

      // Let everyone know
      HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletMessageType.DEVICE_DETACHED);

    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  protected MessageEvent readFromDevice() {

    ByteBuffer messageBuffer = ByteBuffer.allocate(32768);

    TrezorMessage.MessageType type;
    int msgSize;
    int received;

    try {

      // Keep reading until synchronized on "##"
      for (; ; ) {
        byte[] buffer = new byte[64];

        received = deviceOptional.get().read(buffer);

        log.debug("< {} bytes", received);
        TrezorMessageUtils.logPacket("<", 0, buffer);

        if (received < 9) {
          continue;
        }

        // Synchronize the buffer on start of new message ('?' is ASCII 63)
        if (buffer[0] != (byte) '?' || buffer[1] != (byte) '#' || buffer[2] != (byte) '#') {
          // Reject packet
          continue;
        }

        // Evaluate the header information (short, int)
        type = TrezorMessage.MessageType.valueOf((buffer[3] << 8 & 0xFF) + buffer[4]);
        msgSize = ((buffer[5] & 0xFF) << 24) + ((buffer[6] & 0xFF) << 16) + ((buffer[7] & 0xFF) << 8) + (buffer[8] & 0xFF);

        // Treat remainder of packet as the protobuf message payload
        messageBuffer.put(buffer, 9, buffer.length - 9);

        break;
      }

      log.debug("< Type: '{}' Message size: '{}' bytes", type.name(), msgSize);

      int packet = 0;
      while (messageBuffer.position() < msgSize) {

        byte[] buffer = new byte[64];
        received = deviceOptional.get().read(buffer);
        packet++;

        log.debug("< (cont) {} bytes", received);
        TrezorMessageUtils.logPacket("<", packet, buffer);

        if (buffer[0] != (byte) '?') {
          log.warn("< Malformed packet length. Expected: '3f' Actual: '{}'. Ignoring.", String.format("%02x", buffer[0]));
          continue;
        }

        // Append the packet payload to the message buffer
        messageBuffer.put(buffer, 1, buffer.length - 1);
      }

      log.debug("Packet complete");

      // Parse the message
      return TrezorMessageUtils.parse(type, Arrays.copyOfRange(messageBuffer.array(), 0, msgSize));

    } catch (IOException e) {
      log.error("Read endpoint failed", e);
    }

    return null;
  }

  /**
   * @return The HID device info, if present
   *
   * @throws java.io.IOException If the USB connection fails
   */
  private Optional<HIDDeviceInfo> locateTrezor() throws IOException {

    // Get the HID manager
    HIDManager hidManager = HIDManager.getInstance();

    // Attempt to list the attached devices
    HIDDeviceInfo[] infos;
    try {
      infos = hidManager.listDevices();
    } catch (Error e) {
      // This is a bit bad but helps with error identification
      if (e.getMessage().contains("iconv_open")) {
        throw new IllegalStateException("Unable to open 'iconv' library. Check it is installed and at version 1.11+.", e);
      }
      if (e.getMessage().contains("iconv")) {
        throw new IllegalStateException("Unable to convert from UTF-8 using 'iconv' library. Check HID library uses UCS-4LE.", e);
      }
      throw new IllegalStateException("Unable to list devices from HID USB driver.", e);
    }
    if (infos == null) {
      throw new IllegalStateException("Unable to access connected device list. Check USB security policy for this account.");
    }

    // Use the default IDs or those supplied externally
    Integer vendorId = this.vendorId.isPresent() ? this.vendorId.get() : RPI_UART_VENDOR_ID;
    Integer productId = this.productId.isPresent() ? this.productId.get() : RPI_UART_PRODUCT_ID;

    // Attempt to locate the required device
    Optional<HIDDeviceInfo> selectedInfo = Optional.absent();
    for (HIDDeviceInfo info : infos) {

      log.debug("Found USB device: {}", info);

      if (vendorId.equals(info.getVendor_id()) &&
        productId.equals(info.getProduct_id())) {

        log.debug("Verified as a Trezor Shield");

        // Allow a wildcard serial number
        if (serialNumber.isPresent()) {
          if (serialNumber.get().equals(info.getSerial_number())) {
            selectedInfo = Optional.of(info);
            break;
          }
        } else {
          // Any serial number is acceptable
          selectedInfo = Optional.of(info);
          break;
        }
      }
    }

    return selectedInfo;

  }

  /**
   * @param buffer Buffer to write to device
   *
   * @return Number of bytes written
   */
  @Override
  public int writeToDevice(byte[] buffer) {

    Preconditions.checkNotNull(buffer, "'buffer' must be present");
    Preconditions.checkNotNull(deviceOptional, "Device is not connected");

    int bytesSent = 0;

    try {
      bytesSent = deviceOptional.get().write(buffer);
      if (bytesSent != buffer.length) {
        log.warn("Invalid data chunk size sent. Expected: " + buffer.length + " Actual: " + bytesSent);
      }

    } catch (IOException e) {
      log.error("Write endpoint submit data failed.", e);
    }

    return bytesSent;
  }
}