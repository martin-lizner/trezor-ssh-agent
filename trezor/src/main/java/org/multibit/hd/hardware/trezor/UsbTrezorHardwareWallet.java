package org.multibit.hd.hardware.trezor;

import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.HardwareEvents;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.multibit.hd.hardware.core.usb.CP211xBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>Trezor implementation to provide the following to applications:</p>
 * <ul>
 * <li>Access to a Trezor device over USB</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class UsbTrezorHardwareWallet extends AbstractTrezorHardwareWallet {

  private static final Integer DEFAULT_USB_VENDOR_ID = 0x10c4;
  private static final Integer DEFAULT_USB_PRODUCT_ID = 0xea80;

  private static final Logger log = LoggerFactory.getLogger(UsbTrezorHardwareWallet.class);

  private Optional<Integer> vendorId = Optional.absent();
  private Optional<Integer> productId = Optional.absent();
  private Optional<String> serialNumber = Optional.absent();

  private DataOutputStream out = null;

  private Optional<HIDDevice> deviceOptional = Optional.absent();

  /**
   * Default constructor for use with dynamic binding
   */
  public UsbTrezorHardwareWallet() {
    this(Optional.<Integer>absent(), Optional.<Integer>absent(), Optional.<String>absent());
  }

  /**
   * <p>Create a new instance of a USB-based Trezor device (standard)</p>
   *
   * @param vendorId     The vendor ID (default is 0x10c4)
   * @param productId    The product ID (default is 0xea80)
   * @param serialNumber The device serial number (default is to accept any)
   */
  public UsbTrezorHardwareWallet(Optional<Integer> vendorId,
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

  }


  @Override
  public void applySpecification(HardwareWalletSpecification specification) {

    super.applySpecification(specification);

    this.vendorId = specification.getVendorId();
    this.productId = specification.getProductId();
    this.serialNumber = specification.getSerialNumber();

  }

  @Override
  public synchronized void connect() {

    Preconditions.checkState(isDeviceConnected(), "Device is already connected");

    try {

      // Attempt to locate an attached Trezor device
      final Optional<HIDDeviceInfo> hidDeviceInfoOptional = locateTrezor();

      if (!hidDeviceInfoOptional.isPresent()) {
        HardwareEvents.fireSystemEvent(SystemMessageType.DEVICE_DISCONNECTED);
        return;
      }
      HIDDeviceInfo hidDeviceInfo = hidDeviceInfoOptional.get();

      // Get the HID manager
      HIDManager hidManager = HIDManager.getInstance();

      // Attempt to open a serial connection to the USB device
      // Open the device
      deviceOptional = Optional.fromNullable(hidManager.openById(
        hidDeviceInfo.getVendor_id(),
        hidDeviceInfo.getProduct_id(),
        hidDeviceInfo.getSerial_number()
      ));

      Preconditions.checkState(deviceOptional.isPresent(), "Unable to open device");

      HIDDevice device = deviceOptional.get();

      log.debug("Selected: {}, {}, {}",
        device.getManufacturerString(),
        device.getProductString(),
        device.getSerialNumberString()
      );

      // Create and configure the USB to UART bridge
      final CP211xBridge uart = new CP211xBridge(device);

      uart.enable(true);
      uart.purge(3);

      // Add unbuffered data streams for easy data manipulation
      out = new DataOutputStream(uart.getOutputStream());
      DataInputStream in = new DataInputStream(uart.getInputStream());

      // Monitor the input stream
      monitorDataInputStream(in);

      // Must have connected to be here
      HardwareEvents.fireSystemEvent(SystemMessageType.DEVICE_CONNECTED);

    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
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
      HardwareEvents.fireSystemEvent(SystemMessageType.DEVICE_DISCONNECTED);

    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private Optional<HIDDeviceInfo> locateTrezor() throws IOException {

    // Get the HID manager
    HIDManager hidManager = HIDManager.getInstance();

    // Attempt to list the attached devices
    HIDDeviceInfo[] infos = hidManager.listDevices();
    if (infos == null) {
      throw new IllegalStateException("Unable to access connected device list. Check USB security policy for this account.");
    }

    Integer vendorId = this.vendorId.isPresent() ? this.vendorId.get() : DEFAULT_USB_VENDOR_ID;
    Integer productId = this.productId.isPresent() ? this.productId.get() : DEFAULT_USB_PRODUCT_ID;

    // Attempt to locate the required device
    Optional<HIDDeviceInfo> selectedInfo = Optional.absent();
    for (HIDDeviceInfo info : infos) {
      if (vendorId.equals(info.getVendor_id()) &&
        productId.equals(info.getProduct_id())) {
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
      HardwareEvents.fireSystemEvent(SystemMessageType.DEVICE_DISCONNECTED);

    }

  }

}