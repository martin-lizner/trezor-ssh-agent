package org.multibit.hd.hardware.trezor.wallets.shield;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.hid4java.*;
import org.hid4java.event.HidServicesEvent;
import org.hid4java.jna.HidApi;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.events.MessageEventType;
import org.multibit.hd.hardware.core.events.MessageEvents;
import org.multibit.hd.hardware.trezor.utils.TrezorMessageUtils;
import org.multibit.hd.hardware.trezor.wallets.AbstractTrezorHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * <p>Trezor implementation to provide the following to applications:</p>
 * <ul>
 * <li>Access to the Trezor RPi emulation device over USB</li>
 * </ul>
 *
 * <p>This class uses <code>hidapi</code> for each platform due to the
 * custom UART-to-USB present on the RPi Shield hardware.</p>
 *
 * <h3>The Trezor Shield is not a primary device any longer so this code is probably out of date</h3>
 *
 * @since 0.0.1
 *  
 */
public class TrezorShieldUsbHardwareWallet extends AbstractTrezorHardwareWallet implements HidServicesListener {

  // Use the Raspberry Pi UART-to-USB bridge identifiers
  private static final Integer RPI_UART_VENDOR_ID = 0x10c4;
  private static final Integer RPI_UART_PRODUCT_ID = 0xea80;

  private static final Logger log = LoggerFactory.getLogger(TrezorShieldUsbHardwareWallet.class);

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

  static {
    Locale.setDefault(Locale.UK);
  }

  /**
   * Default constructor for use with dynamic binding
   */
  public TrezorShieldUsbHardwareWallet() {
    this(Optional.<Integer>absent(), Optional.<Integer>absent(), Optional.<String>absent());

  }

  /**
   * <p>Create a new instance of a USB-based Trezor emulator running on a Raspberry Pi with the Shield hardware</p>
   *
   * @param vendorId     The vendor ID (default is 0x10c4)
   * @param productId    The product ID (default is 0xea80)
   * @param serialNumber The device serial number (default is to accept any)
   */
  public TrezorShieldUsbHardwareWallet(
    Optional<Integer> vendorId,
    Optional<Integer> productId,
    Optional<String> serialNumber) {

    this.vendorId = vendorId.isPresent() ? vendorId : Optional.of(RPI_UART_VENDOR_ID);
    this.productId = productId.isPresent() ? productId : Optional.of(RPI_UART_PRODUCT_ID);
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
      log.error("Failed to connect device due to USB verification problem");
      return false;
    }

    // Must be OK to be here
    return true;

  }

  @Override
  public synchronized boolean connect() {

    // Expect a present device to be here

    HidDevice device = locatedDevice.get();

    log.debug(
      "Selected: {}, {}, {}",
      device.getVendorId(),
      device.getProductId(),
      device.getSerialNumber()
    );

    // Attempt to open UART communications
    try {
      return attachDevice(device);
    } catch (IOException e) {
      log.error("Failed to attach device due to problem reading UART data stream", e);
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_FAILED);
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
  private boolean attachDevice(HidDevice device) throws IOException {

    byte[] featureReport;

    // Create and configure the USB to UART bridge

    // Reset the device
    featureReport = new byte[]{0x00};
    int bytesSent = device.sendFeatureReport(featureReport, (byte) 0x040);
    // There is a security risk to raising this logging level beyond trace
    log.trace("> UART Reset: {} '{}'", bytesSent, featureReport);

    // Enable the UART
    featureReport = new byte[]{0x01};
    bytesSent = device.sendFeatureReport(featureReport, (byte) 0x041);
    // There is a security risk to raising this logging level beyond trace
    log.trace("> UART Enable: {} '{}'", bytesSent, featureReport);

    // Purge Rx and Tx buffers
    featureReport = new byte[]{0x03};
    bytesSent = device.sendFeatureReport(featureReport, (byte) 0x043);
    // There is a security risk to raising this logging level beyond trace
    log.trace("> Purge RxTx: {} '{}'", bytesSent, featureReport);

    // Must have connected to be here
    MessageEvents.fireMessageEvent(MessageEventType.DEVICE_CONNECTED);

    return true;

  }


  @Override
  public synchronized void softDetach() {

    // Attempt to close the connection (also closes the in/out streams)
    if (locatedDevice.isPresent()) {
      locatedDevice.get().close();
    }

    log.info("Disconnected from Trezor");

    // Let everyone know
    MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DETACHED);
  }

  @Override
  public void hardDetach() {

    log.debug("Hard detach");

    log.debug("Reset endpoints");
    if (locatedDevice.isPresent()) {
      locatedDevice.get().close();
    }

    locatedDevice = Optional.absent();

    log.debug("Exited HID API");
    HidApi.exit();

    log.info("Hard detach complete");

    // Let everyone know
    MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DETACHED_HARD);

  }

  @Override
  protected Optional<MessageEvent> readFromDevice(int duration, TimeUnit timeUnit) {

    ByteBuffer messageBuffer = ByteBuffer.allocate(32768);

    TrezorMessage.MessageType type;
    int msgSize;
    int received;

    // Keep reading until synchronized on "##"
    for (; ; ) {
      byte[] buffer = new byte[64];

      received = locatedDevice.get().read(buffer);

      if (received == -1) {
        return null;
      }

      // There is a security risk to raising this logging level beyond trace
      log.trace("< {} bytes", received);
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

    // There is a security risk to raising this logging level beyond trace
    log.trace("< Type: '{}' Message size: '{}' bytes", type.name(), msgSize);

    int packet = 0;
    while (messageBuffer.position() < msgSize) {

      byte[] buffer = new byte[64];
      received = locatedDevice.get().read(buffer);
      packet++;

      // There is a security risk to raising this logging level beyond trace
      log.trace("< (cont) {} bytes", received);
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

  /**
   * @param buffer Buffer to write to device
   *
   * @return Number of bytes written
   */
  @Override
  public int writeToDevice(byte[] buffer) {

    Preconditions.checkNotNull(buffer, "'buffer' must be present");
    Preconditions.checkNotNull(locatedDevice, "Device is not connected");

    int bytesSent = 0;

    bytesSent = locatedDevice.get().write(buffer, 64, (byte) 0x00);
    if (bytesSent != buffer.length) {
      log.warn("Invalid data chunk size sent. Expected: " + buffer.length + " Actual: " + bytesSent);
    }

    return bytesSent;
  }

  @Override
  public void hidDeviceAttached(HidServicesEvent event) {

  }

  @Override
  public void hidDeviceDetached(HidServicesEvent event) {

  }

  @Override
  public void hidFailure(HidServicesEvent event) {

  }
}