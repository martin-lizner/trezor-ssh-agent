package org.multibit.hd.hardware.trezor;

import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.multibit.hd.hardware.core.HardwareWalletException;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.wallets.AbstractHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.nio.ByteBuffer;

/**
 * <p>Abstract base class provide the following to Trezor hardware wallets:</p>
 * <ul>
 * <li>Access to common methods</li>
 * </ul>
 *
 * <p>The Trezor generally uses USB HID framing and protocol buffer messages</p>
 */
public abstract class AbstractTrezorHardwareWallet extends AbstractHardwareWallet<TrezorMessage.MessageType> {

  private static final Logger log = LoggerFactory.getLogger(AbstractTrezorHardwareWallet.class);


  @Override
  public HardwareWalletSpecification getDefaultSpecification() {

    HardwareWalletSpecification specification = new HardwareWalletSpecification(this.getClass().getCanonicalName());
    specification.setName("TREZOR The Bitcoin Safe");
    specification.setDescription("The hardware Bitcoin wallet. A step in the evolution of Bitcoin towards a completely safe payment system.");
    specification.setHost("192.168.0.8");
    specification.setPort(3000);

    return specification;
  }

  @Override
  public synchronized void disconnect() {

    internalClose();
    hardwareWalletMonitorService.shutdownNow();

  }

  @Override
  public Message readMessage() {

    return readFromDevice();

  }

  public synchronized Message parseTrezorMessage(DataInputStream in) throws HardwareWalletException {
//    // Very broad try-catch because a lot of things can go wrong here and need to be reported
//    try {
//
//      // Read and throw away the magic header markers
//      in.readByte();
//      in.readByte();
//
//      // Read the header code and select a suitable parser
//      final Short headerCode = in.readShort();
//      log.debug("Seeing a trezor message with headerCode " + headerCode);
//      final TrezorMessage.MessageType trezorMessageType = TrezorMessageUtils.getMessageTypeByHeaderCode(headerCode);
//      log.debug("Seeing a trezorMessageType " + trezorMessageType);
//    }

    // TODO - Can probably replace with Trezor Utils parse
    return null;
  }

  public void writeMessage(Message message) {

    ByteBuffer messageBuffer = TrezorMessageUtils.formatAsHIDPackets(message);

    int packets = messageBuffer.position() / 63;
    log.info("Writing {} packets", packets);
    messageBuffer.rewind();

    // HID requires 64 byte packets with 63 bytes of payload
    for (int i = 0; i < packets; i++) {

      byte[] buffer = new byte[64];
      buffer[0] = 63; // Length
      messageBuffer.get(buffer, 1, 63); // Payload

      // Describe the packet
      String s = "Packet [" + i + "]: ";
      for (int j = 0; j < 64; j++) {
        s += String.format(" %02x", buffer[j]);
      }

      log.info("> {}", s);

      writeToDevice(buffer);

      // TODO Debug only
      readMessage();

    }
  }

  /**
   * <p>Implementations should handle their own shutdown before their threads are terminated</p>
   */
  public abstract void internalClose();

  /**
   * <p>Read a complete message buffer from the device and convert it into a protobuf message.</p>
   *
   * @return The protobuf message read from the device
   */
  protected abstract Message readFromDevice();

  /**
   * <p>Write a complete message buffer to the device.</p>
   *
   * @param buffer The buffer that will be written to the device
   *
   * @return The number of bytes written
   */
  protected abstract int writeToDevice(byte[] buffer);

}
