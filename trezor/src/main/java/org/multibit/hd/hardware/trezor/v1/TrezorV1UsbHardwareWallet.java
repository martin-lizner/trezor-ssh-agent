package org.multibit.hd.hardware.trezor.v1;

import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.multibit.hd.hardware.trezor.AbstractTrezorHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.usb.*;
import java.io.IOException;
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
  private UsbEndpoint epr;

  /**
   * Write endpoint
   */
  private UsbEndpoint epw;

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
          HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_FAILURE);
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
    if (epr == null || epw == null) {
      log.error("Device read/write endpoints have not been set.");
      HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_FAILURE);
      return false;
    }

    log.info("Selected trezor OK");

//    try {
//      log.debug("Selected: '{}' '{}' '{}'",
//        device.getManufacturerString(),
//        device.getProductString(),
//        device.getSerialNumberString()
//      );
//
//    } catch (UsbException | UnsupportedEncodingException e) {
//      log.error("Failed to connect device due to problem reading device information", e);
//      HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_FAILURE);
//      return false;
//    }

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
      UsbEndpoint readEndpoint = null;
      UsbEndpoint writeEndpoint = null;

      // Process all endpoints
      for (UsbEndpoint ep : (List<UsbEndpoint>) iface.getUsbEndpoints()) {

        log.debug("Analysing endpoint. Direction: {}, Address: {}", ep.getDirection(), ep.getUsbEndpointDescriptor().bEndpointAddress());

        // Is this an interface with address 0x81?
        if (readEndpoint == null && ep.getDirection() == UsbConst.ENDPOINT_DIRECTION_IN && ep.getUsbEndpointDescriptor().bEndpointAddress() == -127) {
          log.debug("Found EPR");
          readEndpoint = ep;
          // Move to the next interface
          continue;
        }

        // Is this an interface with address 0x01?
        if (writeEndpoint == null && ep.getDirection() == UsbConst.ENDPOINT_DIRECTION_OUT && ep.getUsbEndpointDescriptor().bEndpointAddress() == 1) {
          log.debug("Found EPW");
          writeEndpoint = ep;
        }

        if (readEndpoint != null && writeEndpoint != null) {
          log.debug("Found EPR and EPW");
          break;
        }
      }

      // All interfaces explored examine the result
      if (readEndpoint == null) {
        log.error("Could not find read endpoint on this interface");
        continue;
      }
      if (writeEndpoint == null) {
        log.error("Could not find write endpoint on this interface");
        continue;
      }

      // Trezor UARTs use 64 byte chunks
      if (readEndpoint.getUsbEndpointDescriptor().wMaxPacketSize() != 64) {
        log.error("Unexpected packet size for read endpoint on this interface");
        continue;
      }
      if (writeEndpoint.getUsbEndpointDescriptor().wMaxPacketSize() != 64) {
        log.error("Unexpected packet size for write endpoint on this interface");
        continue;
      }

      epr = readEndpoint;
      epw = writeEndpoint;

      log.info("Verified Trezor device. EPR: {}, EPW: {}.", epr, epw);

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
   * @param device The HID device to communicate with
   *
   * @return True if the device responded to the UART serial initialisation
   *
   * @throws IOException If something goes wrong
   */
  private boolean attachDevice(UsbDevice device) throws IOException {

    // Create and configure the USB to UART bridge
//    final CP211xTransport uart = new CP211xTransport(device);

    //   uart.enable(true);
    //   uart.purge(3);

    // Add unbuffered data streams for easy data manipulation
    //   out = new DataOutputStream(uart.getOutputStream());
    //   DataInputStream in = new DataInputStream(uart.getInputStream());

    // Monitor the input stream
    //   monitorDataInputStream(in);

    // Must have connected to be here
    HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_CONNECTED);

    return true;

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
//    try {
    //deviceOptional.get().close();

    log.info("Disconnected from Trezor");

    // Let everyone know
    HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_DISCONNECTED);
  }

  @Override
  public void sendMessage(Message message) {

    Preconditions.checkNotNull(message, "Message must be present");
    Preconditions.checkNotNull(deviceOptional, "Device is not connected");

    Message response = messageWrite(message);

    log.info("Response was: '{}'", response);

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
    return "USB Trezor (Serial: " + this.serial + ")";
  }

  private Message messageWrite(Message msg) {

    int msgSize = msg.getSerializedSize();
    String msgName = msg.getClass().getSimpleName();
    int msgId = TrezorMessage.MessageType.valueOf("MessageType_" + msgName).getNumber();

    log.info("Writing message: '{}' ({} bytes)", msgName, msgSize);

    ByteBuffer messageBuffer = ByteBuffer.allocate(32768);
    messageBuffer.put((byte) '#');
    messageBuffer.put((byte) '#');
    messageBuffer.put((byte) ((msgId >> 8) & 0xFF));
    messageBuffer.put((byte) (msgId & 0xFF));
    messageBuffer.put((byte) ((msgSize >> 24) & 0xFF));
    messageBuffer.put((byte) ((msgSize >> 16) & 0xFF));
    messageBuffer.put((byte) ((msgSize >> 8) & 0xFF));
    messageBuffer.put((byte) (msgSize & 0xFF));
    messageBuffer.put(msg.toByteArray());
    while (messageBuffer.position() % 63 > 0) {
      messageBuffer.put((byte) 0);
    }

    UsbPipe outPipe = epw.getUsbPipe();
    try {
      outPipe.open();

      int chunks = messageBuffer.position() / 63;
      log.info("Writing {} chunks", chunks);
      messageBuffer.rewind();

      // Break the data into 63 byte chunks with length prefix
      for (int i = 0; i < chunks; i++) {

        byte[] buffer = new byte[64];
        buffer[0] = (byte) '?';
        messageBuffer.get(buffer, 1, 63);

        // Describe the chunk
        String s = "chunk:";
        for (int j = 0; j < 64; j++) {
          s += String.format(" %02x", buffer[j]);
        }

        log.info("> {}", s);

        int sent = outPipe.syncSubmit(buffer);
        if (sent != buffer.length) {
          throw new UsbException("Invalid data chunk size sent: " + sent);
        }
      }
    } catch (UsbException e) {
      // TODO Better error handling here
      e.printStackTrace();
    } finally {
      try {
        outPipe.close();
      } catch (UsbException e) {
        // Do nothing
      }
    }

    return messageRead();

  }

  private Message messageRead() {

    ByteBuffer messageBuffer = ByteBuffer.allocate(32768);
    ByteBuffer headerBuffer = ByteBuffer.allocate(4);
    byte[] buffer = new byte[64];

    TrezorMessage.MessageType type;
    int msgSize;
    int received;

    log.debug("Opening EPR pipe");
    UsbPipe inPipe = epr.getUsbPipe();
    try {
      inPipe.open();

      for (; ; ) {
        received = inPipe.syncSubmit(buffer);

        log.debug("Read chunk: {} bytes", received);

        if (received < 9) {
          continue;
        }


        if (buffer[0] != (byte) '?' || buffer[1] != (byte) '#' || buffer[2] != (byte) '#') {
          continue;
        }

        type = TrezorMessage.MessageType.valueOf((buffer[3] << 8) + buffer[4]);
        msgSize = toInt(headerBuffer.put(buffer[5]).put(buffer[6]).put(buffer[7]).put(buffer[8]).array());
        messageBuffer.put(buffer, 9, buffer.length - 9);

        break;
      }

      log.debug("< Type: '{}' Message size: '{}' bytes", type.name(), msgSize);

      while (messageBuffer.position() < msgSize) {

        received = inPipe.syncSubmit(buffer);

        buffer = new byte[64];
        log.debug("Read chunk (cont): {} bytes", received);

        if (buffer[0] != (byte) '?') continue;
        messageBuffer.put(buffer, 1, buffer.length - 1);
      }

      return parseMessageFromBytes(type, Arrays.copyOfRange(messageBuffer.array(), 0, msgSize));

    } catch (UsbException e) {
      log.debug("Opening EPR pipe");
    } finally {
      try {
        inPipe.close();
      } catch (UsbException e) {
        // Do nothing
      }
    }

    // TODO Better error handling here
    return null;

  }

  /**
   * @param type The message type
   * @param data The data payload
   *
   * @return The message if it could be parsed
   */
  private Message parseMessageFromBytes(TrezorMessage.MessageType type, byte[] data) {

    Message msg = null;
    log.info("Parsing '{}' ({} bytes):", type, data.length);

    String s = "data:";
    for (byte aData : data) {
      s += String.format(" %02x", aData);
    }
    log.info("Trezor.parseMessageFromBytes()", s);

    try {
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_Success_VALUE) {
        msg = TrezorMessage.Success.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_Failure_VALUE) {
        msg = TrezorMessage.Failure.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_Entropy_VALUE) {
        msg = TrezorMessage.Entropy.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_PublicKey_VALUE) {
        msg = TrezorMessage.PublicKey.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_Features_VALUE) {
        msg = TrezorMessage.Features.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_PinMatrixRequest_VALUE) {
        msg = TrezorMessage.PinMatrixRequest.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_TxRequest_VALUE) {
        msg = TrezorMessage.TxRequest.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_ButtonRequest_VALUE) {
        msg = TrezorMessage.ButtonRequest.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_Address_VALUE) {
        msg = TrezorMessage.Address.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_EntropyRequest_VALUE) {
        msg = TrezorMessage.EntropyRequest.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_MessageSignature_VALUE) {
        msg = TrezorMessage.MessageSignature.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_PassphraseRequest_VALUE) {
        msg = TrezorMessage.PassphraseRequest.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_TxSize_VALUE) {
        msg = TrezorMessage.TxSize.parseFrom(data);
      }
      if (type.getNumber() == TrezorMessage.MessageType.MessageType_WordRequest_VALUE) {
        msg = TrezorMessage.WordRequest.parseFrom(data);
      }
    } catch (InvalidProtocolBufferException e) {
      log.error("Could not parse message", e);
      return null;
    }

    return msg;
  }

  public static int toInt(byte[] bytes) {
    int ret = 0;
    for (int i = 0; i < 4 && i < bytes.length; i++) {
      ret <<= 8;
      ret |= (int) bytes[i] & 0xFF;
    }
    return ret;
  }
}