package org.multibit.hd.hardware.trezor.v1;

import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.multibit.hd.hardware.core.usb.STM32F205xBridge;
import org.multibit.hd.hardware.trezor.AbstractTrezorHardwareWallet;
import org.multibit.hd.hardware.trezor.TrezorMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>Trezor implementation to provide the following to applications:</p>
 * <ul>
 * <li>Access to a version 1 production Trezor device over USB</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1UsbHardwareWallet extends AbstractTrezorHardwareWallet {

  private static final Integer SATOSHI_LABS_VENDOR_ID = 0x534c; // 21324 dec
  private static final Integer TREZOR_V1_PRODUCT_ID = 1;

  private static final Logger log = LoggerFactory.getLogger(TrezorV1UsbHardwareWallet.class);

  private Optional<Integer> vendorId = Optional.absent();
  private Optional<Integer> productId = Optional.absent();
  private Optional<String> serialNumber = Optional.absent();

  private DataOutputStream out = null;

  private Optional<HIDDevice> deviceOptional = Optional.absent();

  /**
   * Default constructor for use with dynamic binding
   */
  public TrezorV1UsbHardwareWallet() {
    this(Optional.<Integer>absent(), Optional.<Integer>absent(), Optional.<String>absent());

  }

  /**
   * <p>Create a new instance of a USB-based Trezor device (standard)</p>
   *
   * @param vendorId     The vendor ID (default is 0x10c4)
   * @param productId    The product ID (default is 0xea80)
   * @param serialNumber The device serial number (default is to accept any)
   */
  public TrezorV1UsbHardwareWallet(Optional<Integer> vendorId,
                                   Optional<Integer> productId,
                                   Optional<String> serialNumber) {

    // Initialise the HID library
    if (!ClassPathLibraryLoader.loadNativeHIDLibrary()) {
      throw new IllegalStateException(
        "Unable to load native USB library. Check class loader permissions/JAR integrity.");
    }

    this.vendorId = vendorId;
    this.productId = productId;
    this.serialNumber = serialNumber;

    initialise();

  }


  @Override
  public void applySpecification(HardwareWalletSpecification specification) {

    super.applySpecification(specification);

    this.vendorId = specification.getVendorId();
    this.productId = specification.getProductId();
    this.serialNumber = specification.getSerialNumber();

  }

  @Override
  public synchronized boolean connect() {

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

    // Must have a present device to be here

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

    // Attempt to open USB HID communications
    try {
      return attachDevice(device);
    } catch (IOException e) {
      log.error("Failed to attach device due to problem reading USB data stream", e);
      HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_FAILURE);
    }

    // Must have failed to be here
    return false;

  }

  /**
   * @param device The HID device to communicate with
   *
   * @return True if the device responded to the UART serial initialisation
   *
   * @throws IOException If something goes wrong
   */
  private boolean attachDevice(HIDDevice device) throws IOException {

    // Create and configure the USB to UART bridge
    final STM32F205xBridge usb = new STM32F205xBridge(device);

    // Add unbuffered data streams for easy data manipulation
    out = new DataOutputStream(usb.getOutputStream());
    DataInputStream in = new DataInputStream(usb.getInputStream());

    // Monitor the input stream
    monitorDataInputStream(in);

    // Must have connected to be here
    HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_CONNECTED);

    return true;

  }

  /**
   * @return A HID device with the given vendor, product and serial number IDs
   *
   * @throws IOException If something goes wrong with the USB
   */
  private Optional<HIDDevice> locateDevice() throws IOException {

    Preconditions.checkState(isDeviceConnected(), "Device is already connected");

    // Attempt to locate an attached Trezor device
    final Optional<HIDDeviceInfo> hidDeviceInfoOptional = locateTrezor();

    // No matching device so indicate that it is disconnected
    if (!hidDeviceInfoOptional.isPresent()) {
      HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_DISCONNECTED);
      return Optional.absent();
    }

    HIDDeviceInfo hidDeviceInfo = hidDeviceInfoOptional.get();

    // Get the HID manager
    HIDManager hidManager = HIDManager.getInstance();

    // Attempt to open a serial connection to the USB device
    return Optional.fromNullable(hidManager.openById(
      hidDeviceInfo.getVendor_id(),
      hidDeviceInfo.getProduct_id(),
      hidDeviceInfo.getSerial_number()
    ));

  }

  /**
   * @return True if device is connected (the HID device is present)
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
      HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_DISCONNECTED);

    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * @return The HID device info, if present
   *
   * @throws IOException If the USB connection fails
   */
  private Optional<HIDDeviceInfo> locateTrezor() throws IOException {

    // Get the HID manager
    HIDManager hidManager = HIDManager.getInstance();

    // Attempt to list the attached devices
    HIDDeviceInfo[] infos;
    try {
      infos = hidManager.listDevices();
    } catch (Error e) {
      // TODO Create collection of descriptive USB exceptions
      throw new IllegalStateException("Unable to access USB due to 'iconv' library returning -1. Check your locale supports conversion from UTF-8 to your native character set.", e);
    }
    if (infos == null) {
      throw new IllegalStateException("Unable to access connected device list. Check USB security policy for this account.");
    }

    // Use the default IDs or those supplied externally
    Integer vendorId = this.vendorId.isPresent() ? this.vendorId.get() : SATOSHI_LABS_VENDOR_ID;
    Integer productId = this.productId.isPresent() ? this.productId.get() : TREZOR_V1_PRODUCT_ID;

    // Attempt to locate the required device
    Optional<HIDDeviceInfo> selectedInfo = Optional.absent();
    for (HIDDeviceInfo info : infos) {

      log.debug("Found device: {}", info);

      if (vendorId.equals(info.getVendor_id()) &&
        productId.equals(info.getProduct_id())) {

        log.debug("Found Trezor: {}", info);

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

  @Override
  public void sendMessage(Message message) {

    Preconditions.checkNotNull(message, "Message must be present");
    Preconditions.checkNotNull(deviceOptional, "Device is not connected");

    try {

      // Apply the message to the data output stream
      TrezorMessageUtils.writeMessage(message, out);

    } catch (IOException e) {

      log.warn("I/O error during write. Closing device.", e);
      HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_DISCONNECTED);

    }

  }

}