package org.multibit.hd.hardware.trezor.wallets.v1;

import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.HardwareWalletMessageType;
import org.multibit.hd.hardware.trezor.utils.TrezorMessageUtils;
import org.multibit.hd.hardware.trezor.wallets.AbstractTrezorHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.usb.*;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

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

  private static final short SATOSHI_LABS_VENDOR_ID = 0x534c;
  private static final short TREZOR_V1_PRODUCT_ID = 0x01;

  private static final Logger log = LoggerFactory.getLogger(TrezorV1UsbHardwareWallet.class);

  private Optional<Integer> vendorId = Optional.absent();
  private Optional<Integer> productId = Optional.absent();
  private Optional<String> serialNumber = Optional.absent();

  //	private UsbDevice device;

  /**
   * Device serial number
   */
  private String serial;

  /**
   * Read endpoint
   */
  private UsbEndpoint readEndpoint;

  /**
   * Write endpoint
   */
  private UsbEndpoint writeEndpoint;

  /**
   * Selected device
   */
  private Optional<UsbDevice> deviceOptional = Optional.absent();

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
  public void initialise() {

  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized boolean connect() {

    final UsbServices services;
    try {

      // Get the USB services and dump information about them
      services = UsbHostManager.getUsbServices();

      // Explore all attached devices including hubs
      locateDevice(services.getRootUsbHub());

    } catch (UsbException e) {
      log.error("Failed to connect device due to USB problem", e);
      return false;
    }

    if (!deviceOptional.isPresent()) {
      log.info("Failed to connect device due to unmatched device (assumed disconnected)");
      return false;
    }

    log.info("Located a Trezor device. Attempting verification...");

    UsbDevice device = deviceOptional.get();

    // Examine the characteristics to ensure it is a Trezor

    // Process all configurations
    for (UsbConfiguration configuration : (List<UsbConfiguration>) device.getUsbConfigurations()) {

      if (configuration.getUsbInterfaces().isEmpty()) {
        log.debug("Configuration does not contain interfaces - skipping.");
        continue;
      }

      // Verify the active configuration
      if (configuration.isActive()) {
        if (!verifyConfiguration(configuration)) {
          log.error("Device not verified or its USB interface could not be claimed.");
          HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletMessageType.DEVICE_FAILURE);
          return false;
        } else {
          // Found a suitable Trezor device
          log.info("Found suitable device.");
          break;
        }
      } else {
        log.debug("Configuration is not active");
      }

    }

    // Check for a connected Trezor
    if (readEndpoint == null || writeEndpoint == null) {
      log.error("Device read/write endpoints have not been set.");
      HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletMessageType.DEVICE_FAILURE);
      return false;
    }

    try {
      log.debug("Selected: '{}' '{}' '{}'",
        device.getManufacturerString(),
        device.getProductString(),
        device.getSerialNumberString()
      );

    } catch (UsbException | UnsupportedEncodingException e) {
      log.error("Failed to connect device due to problem reading device information", e);
      HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletMessageType.DEVICE_FAILURE);
      return false;
    }

    // Must be OK to be here
    return true;

  }

  @SuppressWarnings("unchecked")
  private boolean verifyConfiguration(UsbConfiguration configuration) {

    // Process all interfaces
    for (UsbInterface iface : (List<UsbInterface>) configuration.getUsbInterfaces()) {

      if (!iface.isActive()) {
        log.debug("Interface is not active so skipping.");
        continue;
      }

      // Temporary endpoints
      UsbEndpoint possibleReadEndpoint = null;
      UsbEndpoint possibleWriteEndpoint = null;

      // Process all endpoints
      for (UsbEndpoint ep : (List<UsbEndpoint>) iface.getUsbEndpoints()) {

        log.debug("Analysing endpoint. Direction: {}, Address: {}",
          String.format("0x%02x", ep.getDirection()),
          String.format("0x%02x", ep.getUsbEndpointDescriptor().bEndpointAddress())
        );

        // Is this an interface with address 0x81?
        if (possibleReadEndpoint == null && ep.getDirection() == UsbConst.ENDPOINT_DIRECTION_IN && ep.getUsbEndpointDescriptor().bEndpointAddress() == -127) {
          log.debug("Found the read endpoint");
          possibleReadEndpoint = ep;
          // Move to the next interface
          continue;
        }

        // Is this an interface with address 0x01?
        if (possibleWriteEndpoint == null && ep.getDirection() == UsbConst.ENDPOINT_DIRECTION_OUT && ep.getUsbEndpointDescriptor().bEndpointAddress() == 1) {
          log.debug("Found the write endpoint");
          possibleWriteEndpoint = ep;
        }

        if (possibleReadEndpoint != null && possibleWriteEndpoint != null) {
          log.debug("Found both endpoints");
          break;
        }
      }

      // All interfaces explored examine the result
      if (possibleReadEndpoint == null) {
        log.error("Could not find read endpoint on this interface");
        continue;
      }
      if (possibleWriteEndpoint == null) {
        log.error("Could not find write endpoint on this interface");
        continue;
      }

      // Trezor uses use 64 byte packet sizes
      if (possibleReadEndpoint.getUsbEndpointDescriptor().wMaxPacketSize() != 64) {
        log.error("Unexpected packet size for read endpoint on this interface");
        continue;
      }
      if (possibleWriteEndpoint.getUsbEndpointDescriptor().wMaxPacketSize() != 64) {
        log.error("Unexpected packet size for write endpoint on this interface");
        continue;
      }

      readEndpoint = possibleReadEndpoint;
      writeEndpoint = possibleWriteEndpoint;

      log.info("Verified Trezor device.");

      try {

        log.info("Attempting to force claim...");
        iface.claim(new UsbInterfacePolicy() {
          @Override
          public boolean forceClaim(UsbInterface usbInterface) {
            return true;
          }
        });

        log.info("Claimed the Trezor device");

        // Stop looking
        return true;

      } catch (UsbException e) {
        log.warn("Failed to claim device. No communication will be possible.", e);
        return false;

      }

    }

    log.warn("All USB interfaces explored and none verify as a Trezor device.");

    // Must have failed to be here
    return false;

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

    log.info("Disconnected from Trezor");

    // Let everyone know
    HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletMessageType.DEVICE_DISCONNECTED, null);

  }

  /**
   * Dumps the specified USB device to stdout.
   *
   * @param device The USB device to dump.
   */
  @SuppressWarnings("unchecked")
  private void locateDevice(final UsbDevice device) {

    if (SATOSHI_LABS_VENDOR_ID == device.getUsbDeviceDescriptor().idVendor() &&
      TREZOR_V1_PRODUCT_ID == device.getUsbDeviceDescriptor().idProduct()) {
      deviceOptional = Optional.of(device);
    }

    // Dump child devices if device is a hub
    if (device.isUsbHub()) {
      final UsbHub hub = (UsbHub) device;
      for (UsbDevice child : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
        locateDevice(child);
      }
    }

  }

  @Override
  public String toString() {
    return "USB Trezor Version 1 (Serial: " + this.serial + ")";
  }

  @Override
  protected int writeToDevice(byte[] buffer) {

    Preconditions.checkNotNull(buffer, "'buffer' must be present");
    Preconditions.checkNotNull(deviceOptional, "Device is not connected");

      UsbPipe outPipe = null;
      try {
        log.debug("Writing buffer to USB pipe...");
        outPipe = writeEndpoint.getUsbPipe();
        outPipe.open();

        int bytesSent = outPipe.syncSubmit(buffer);
        if (bytesSent != buffer.length) {
          throw new UsbException("Invalid packet size sent. Expected: " + buffer.length + " Actual: " + bytesSent);
        }

        log.debug("Wrote {} bytes to USB pipe.", bytesSent);
        return bytesSent;

      } catch (UsbException e) {
        log.error("Write endpoint submit data failed.", e);
      } finally {
        try {
          if (outPipe != null) {
            outPipe.close();
          }
        } catch (UsbException e) {
          log.warn("Failed to close the write endpoint", e);
        }
      }

    return 0;
  }

  @Override
  protected synchronized Message readFromDevice() {

    log.debug("Reading from hardware device");

    ByteBuffer messageBuffer = ByteBuffer.allocate(32768);

    TrezorMessage.MessageType type;
    int msgSize;
    int received;

    UsbPipe inPipe = null;
    try {

      inPipe = readEndpoint.getUsbPipe();
      if (inPipe == null) {
        throw new UsbException("Read endpoint get pipe failed.");
      }

      if (!inPipe.isOpen()) {
        log.debug("Read endpoint open pipe");
        inPipe.open();
      }

      // Keep reading until synchronized on "##"
      for (; ; ) {
        byte[] buffer = new byte[64];

        received = inPipe.syncSubmit(buffer);

        log.debug("< {} bytes", received);
        TrezorMessageUtils.logPacket("<", 0, buffer);

        if (received < 9) {
          continue;
        }

        // Synchronize the buffer on start of new message ('?' is ASCII 63)
        if (buffer[0] != (byte) '?' || buffer[1] != (byte) '#' || buffer[2] != (byte) '#') {
          // Reject packet
          log.debug("Rejecting message (not synchronized)");
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
        received = inPipe.syncSubmit(buffer);
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

    } catch (UsbException e) {
      log.error("Read endpoint failed", e);
    } finally {
      try {
        if (inPipe != null) {
          inPipe.close();
        }
      } catch (UsbException e) {
        log.warn("Failed to close the read endpoint", e);
      }
    }

    // TODO Better error handling here
    return null;

  }

}