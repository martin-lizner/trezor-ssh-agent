package org.multibit.hd.hardware.trezor.wallets.v1;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.hid4java.*;
import org.hid4java.event.HidServicesEvent;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.events.MessageEventType;
import org.multibit.hd.hardware.core.events.MessageEvents;
import org.multibit.hd.hardware.trezor.utils.TrezorMessageUtils;
import org.multibit.hd.hardware.trezor.wallets.AbstractTrezorHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * <p>Trezor implementation to provide the following to applications:</p>
 * <ul>
 * <li>Access to a version 1 production Trezor device over USB using the HID API</li>
 * </ul>
 * <p>This uses a hybrid approach between libusb and hidapi</p>
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1HidHardwareWallet extends AbstractTrezorHardwareWallet implements HidServicesListener {

  private static final Integer SATOSHI_LABS_VENDOR_ID = 0x534c;
  private static final Integer TREZOR_V1_PRODUCT_ID = 0x01;
  private static final int PACKET_LENGTH = 64;

  private static final Logger log = LoggerFactory.getLogger(TrezorV1HidHardwareWallet.class);

  private Optional<Integer> vendorId = Optional.absent();
  private Optional<Integer> productId = Optional.absent();
  private Optional<String> serialNumber = Optional.absent();

  /**
   * The located device
   */
  private Optional<HidDevice> locatedDevice = Optional.absent();

  /**
   * The USB HID entry point
   */
  private final HidServices hidServices;

  /**
   * Default constructor for use with dynamic binding
   */
  public TrezorV1HidHardwareWallet() {
    this(Optional.<Integer>absent(), Optional.<Integer>absent(), Optional.<String>absent());
  }

  /**
   * <p>Create a new instance of a USB-based Trezor device (standard)</p>
   *
   * @param vendorId     The vendor ID (default is 0x534c)
   * @param productId    The product ID (default is 0x01)
   * @param serialNumber The device serial number (default is to accept any)
   */
  public TrezorV1HidHardwareWallet(
    Optional<Integer> vendorId,
    Optional<Integer> productId,
    Optional<String> serialNumber) {

    this.vendorId = vendorId.isPresent() ? vendorId : Optional.of(SATOSHI_LABS_VENDOR_ID);
    this.productId = productId.isPresent() ? productId : Optional.of(TREZOR_V1_PRODUCT_ID);
    this.serialNumber = serialNumber;

    try {

      // Get the USB services and dump information about them
      hidServices = HidManager.getHidServices();
      hidServices.addHidServicesListener(this);

    } catch (HidException e) {
      log.error("Failed to create client due to USB services problem", e);
      throw new IllegalStateException("Failed to create client due to HID services problem", e);
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

    // Ensure we close any earlier connections
    if (locatedDevice.isPresent()) {
      softDetach();
    }

    // Explore all attached HID devices
    locatedDevice = Optional.fromNullable(
      hidServices.getHidDevice(
        vendorId.get(),
        productId.get(),
        serialNumber.orNull()
      )
    );

    if (!locatedDevice.isPresent()) {
      log.warn("Device not attached");
    }

    // Must be OK to be here
    return true;

  }

  @Override
  public synchronized void softDetach() {

    log.debug("Reset endpoints");
    if (locatedDevice.isPresent()) {
      locatedDevice.get().close();
    }

    locatedDevice = Optional.absent();

    log.info("Detached from Trezor");

  }

  @Override
  public void hardDetach() {

    log.debug("Hard detach");

    log.debug("Reset endpoints");
    if (locatedDevice.isPresent()) {
      locatedDevice.get().close();
    }

    locatedDevice = Optional.absent();

    hidServices.removeUsbServicesListener(this);

    log.info("Hard detach complete");

    // Let everyone know
    MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DETACHED_HARD);

  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized boolean connect() {

    // Check if the verify environment located a Trezor
    if (!locatedDevice.isPresent()) {
      log.debug("Suspect recently detached device. Attempting to locate...");

      // Explore all attached devices including hubs to verify USB library is working
      // and to determine initial state
      attach();

      if (!locatedDevice.isPresent()) {
        log.debug("Failed to locate. Device must be detached.");
        MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DETACHED);
        return false;
      }

    }

    log.info("Located a Trezor device. Attempting verification...");

    // Must be OK to be here
    return true;

  }

  @Override
  public String toString() {
    if (locatedDevice.isPresent()) {
      return "USB Trezor Version 1: " + locatedDevice.get().getId();
    }
    return "Not attached";
  }

  @Override
  protected int writeToDevice(byte[] buffer) {

    Preconditions.checkNotNull(buffer, "'buffer' must be present");

    Preconditions.checkNotNull(locatedDevice, "Device is not located");
    Preconditions.checkState(locatedDevice.isPresent(), "Device is not connected");

    log.debug("Writing buffer to HID pipe...");

    int bytesSent = locatedDevice.get().write(
      buffer,
      PACKET_LENGTH,
      (byte) 0x00
    );
    if (bytesSent != buffer.length) {
      log.warn("Invalid packet size sent. Expected: " + buffer.length + " Actual: " + bytesSent);
    }

    log.debug("Wrote {} bytes to USB pipe.", bytesSent);

    return bytesSent;

  }

  @Override
  protected synchronized Optional<MessageEvent> readFromDevice() {

    log.debug("Reading from hardware device");

    ByteBuffer messageBuffer = ByteBuffer.allocate(32768);

    TrezorMessage.MessageType type;
    int msgSize;
    int received;

    // Keep reading until synchronized on "##"
    for (; ; ) {
      byte[] buffer = new byte[PACKET_LENGTH];

      received = locatedDevice.get().read(buffer);

      log.debug("< {} bytes", received);

      if (received == -1) {
        return Optional.absent();
      }

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

      byte[] buffer = new byte[PACKET_LENGTH];
      received = locatedDevice.get().read(buffer);
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
    return Optional.of(TrezorMessageUtils.parse(type, Arrays.copyOfRange(messageBuffer.array(), 0, msgSize)));

  }

  @Override
  public void hidDeviceAttached(HidServicesEvent event) {

    HidDeviceInfo attachedDevice = event.getHidDeviceInfo();

    int attachedVendorId = (int) attachedDevice.getVendorId();
    int attachedProductId = (int) attachedDevice.getProductId();

    // Check if it is a device we're interested in that was attached
    if (vendorId.get().equals(attachedVendorId) &&
      productId.get().equals(attachedProductId)) {
      // Inform others of this event
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_ATTACHED);
    }

  }

  @Override
  public void hidDeviceDetached(HidServicesEvent event) {

    HidDeviceInfo attachedDevice = event.getHidDeviceInfo();

    int detachedVendorId = (int) attachedDevice.getVendorId();
    int detachedProductId = (int) attachedDevice.getProductId();

    // Check if it is a device we're interested in that was detached
    if (vendorId.get().equals(detachedVendorId) &&
      productId.get().equals(detachedProductId)) {
      // Inform others of this event
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DETACHED);
    }

  }

  @Override
  public void hidFailure(HidServicesEvent event) {

    MessageEvents.fireMessageEvent(MessageEventType.DEVICE_FAILED);

  }
}