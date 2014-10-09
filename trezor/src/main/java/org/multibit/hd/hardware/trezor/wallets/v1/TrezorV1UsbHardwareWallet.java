package org.multibit.hd.hardware.trezor.wallets.v1;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.events.MessageEventType;
import org.multibit.hd.hardware.core.events.MessageEvents;
import org.multibit.hd.hardware.trezor.utils.TrezorMessageUtils;
import org.multibit.hd.hardware.trezor.wallets.AbstractTrezorHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.usb.*;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;
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
public class TrezorV1UsbHardwareWallet extends AbstractTrezorHardwareWallet implements UsbServicesListener {

  private static final Short SATOSHI_LABS_VENDOR_ID = (short) 0x534c;
  private static final Short TREZOR_V1_PRODUCT_ID = 0x01;

  private static final Logger log = LoggerFactory.getLogger(TrezorV1UsbHardwareWallet.class);

  private Optional<Short> vendorId = Optional.absent();
  private Optional<Short> productId = Optional.absent();
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
  private Optional<UsbDevice> locatedDevice = Optional.absent();

  /**
   * The USB services entry point
   */
  private final UsbServices usbServices;

  /**
   * Default constructor for use with dynamic binding
   */
  public TrezorV1UsbHardwareWallet() {
    this(Optional.<Short>absent(), Optional.<Short>absent(), Optional.<String>absent());

  }

  /**
   * <p>Create a new instance of a USB-based Trezor device (standard)</p>
   *
   * @param vendorId     The vendor ID (default is 0x534c)
   * @param productId    The product ID (default is 0x01)
   * @param serialNumber The device serial number (default is to accept any)
   */
  public TrezorV1UsbHardwareWallet(Optional<Short> vendorId,
                                   Optional<Short> productId,
                                   Optional<String> serialNumber) {

    this.vendorId = vendorId.isPresent() ? vendorId : Optional.of(SATOSHI_LABS_VENDOR_ID);
    this.productId = productId.isPresent() ? productId : Optional.of(TREZOR_V1_PRODUCT_ID);
    this.serialNumber = serialNumber;

    try {

      // Get the USB services and dump information about them
      usbServices = UsbHostManager.getUsbServices();
      usbServices.addUsbServicesListener(this);

    } catch (UsbException e) {
      log.error("Failed to create client due to USB services problem", e);
      throw new IllegalStateException("Failed to create client due to USB services problem");
    }

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
  public boolean attach() {

    try {

      // Explore all attached devices including hubs to verify USB library is working
      // and to determine initial state
      locateDevice(usbServices.getRootUsbHub());

      // Must be OK to be here
      return true;

    } catch (UsbException e) {
      log.error("Failed to connect device due to USB verification problem", e);
      return false;
    }

  }

  @Override
  public synchronized void detach() {

    readEndpoint = null;
    writeEndpoint = null;
    log.debug("Reset endpoints");

    locatedDevice = Optional.absent();

    log.info("Detached from Trezor");

  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized boolean connect() {

    // Check if the verify environment located a Trezor
    if (!locatedDevice.isPresent()) {
      log.debug("Suspect detached device. Attempting to locate...");
      try {

        // Explore all attached devices including hubs to verify USB library is working
        // and to determine initial state
        locateDevice(usbServices.getRootUsbHub());

        if (!locatedDevice.isPresent()) {
          log.debug("Failed to locate. Device must be detached.");
          MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DETACHED);
          return false;
        }

      } catch (UsbException e) {
        log.error("Failed to connect device due to USB problem", e);
        MessageEvents.fireMessageEvent(MessageEventType.DEVICE_FAILED);
        return false;
      }

    }

    log.info("Located a Trezor device. Attempting verification...");

    UsbDevice device = locatedDevice.get();

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
          MessageEvents.fireMessageEvent(MessageEventType.DEVICE_FAILED);
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
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_FAILED);
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

      // Check for a claimed device
      if (iface.isClaimed()) {

        log.info("Device is already claimed. Assuming a re-attachment.");
        return true;
      } else {

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

        } catch (Exception e) {
          log.warn("Failed to claim device. No communication will be possible.", e);
          return false;

        }
      }

    }

    log.warn("All USB interfaces explored and none verify as a Trezor device.");
    MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DETACHED);

    // Must have failed to be here
    return false;

  }

  /**
   * Dumps the specified USB device to stdout.
   *
   * @param device The USB device to dump.
   */
  @SuppressWarnings("unchecked")
  private void locateDevice(final UsbDevice device) {

    if (vendorId.get() == device.getUsbDeviceDescriptor().idVendor() &&
      productId.get() == device.getUsbDeviceDescriptor().idProduct()) {
      locatedDevice = Optional.of(device);
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

    Preconditions.checkNotNull(locatedDevice, "Device is not located");
    Preconditions.checkState(locatedDevice.isPresent(), "Device is not connected");

    Preconditions.checkNotNull(writeEndpoint, "Endpoints have not been initialised");

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
  protected synchronized MessageEvent readFromDevice() {

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

  @Override
  public void usbDeviceAttached(UsbServicesEvent event) {

    UsbDevice attachedDevice = event.getUsbDevice();

    // Check if it is a device we're interested in that was attached
    if (vendorId.get().equals(attachedDevice.getUsbDeviceDescriptor().idVendor()) &&
      productId.get().equals(attachedDevice.getUsbDeviceDescriptor().idProduct())) {
      // Inform others of this event
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_ATTACHED);
    }

  }

  @Override
  public void usbDeviceDetached(UsbServicesEvent event) {

    UsbDevice disconnectedDevice = event.getUsbDevice();

    // Check if it is our device that was detached
    if (vendorId.get().equals(disconnectedDevice.getUsbDeviceDescriptor().idVendor()) &&
      productId.get().equals(disconnectedDevice.getUsbDeviceDescriptor().idProduct())) {
      // Inform others of this event
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DETACHED);
    }

  }

}