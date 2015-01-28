package org.multibit.hd.hardware.trezor.wallets;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.wallets.AbstractHardwareWallet;
import org.multibit.hd.hardware.trezor.utils.TrezorMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * <p>Abstract base class provide the following to Trezor hardware wallets:</p>
 * <ul>
 * <li>Access to common methods</li>
 * </ul>
 *
 * <p>The Trezor generally uses USB HID framing and protocol buffer messages</p>
 */
public abstract class AbstractTrezorHardwareWallet extends AbstractHardwareWallet {

  private static final Logger log = LoggerFactory.getLogger(AbstractTrezorHardwareWallet.class);

  @Override
  public HardwareWalletSpecification getDefaultSpecification() {

    HardwareWalletSpecification specification = new HardwareWalletSpecification(this.getClass().getCanonicalName());
    specification.setName("TREZOR The Bitcoin Safe");
    specification.setDescription("The hardware Bitcoin wallet. A step in the evolution of Bitcoin towards a completely safe payment system.");

    return specification;
  }

  @Override
  public void disconnect() {

    // A disconnect has the same behaviour as a soft detach
    softDetach();

  }

  @Override
  public Optional<MessageEvent> readMessage(int duration, TimeUnit timeUnit) {

    return readFromDevice(duration, timeUnit);

  }

  @Override
  @SuppressFBWarnings(value = {"SBSC_USE_STRINGBUFFER_CONCATENATION"}, justification = "Only occurs at trace")
  public void writeMessage(Message message) {

    ByteBuffer messageBuffer = TrezorMessageUtils.formatAsHIDPackets(message);

    int packets = messageBuffer.position() / 63;
    log.debug("Writing {} packets", packets);
    messageBuffer.rewind();

    // HID requires 64 byte packets with 63 bytes of payload
    for (int i = 0; i < packets; i++) {

      byte[] buffer = new byte[64];
      buffer[0] = 63; // Length
      messageBuffer.get(buffer, 1, 63); // Payload

      if (log.isTraceEnabled()) {
        // Describe the packet
        String s = "Packet [" + i + "]: ";
        for (int j = 0; j < 64; j++) {
          s += String.format(" %02x", buffer[j]);
        }

        // There is a security risk to raising this logging level beyond trace
        log.trace("> {}", s);
      }

      writeToDevice(buffer);

    }
  }

  /**
   * <p>Read a complete message buffer from the device and convert it into a Core message.</p>
   *
   * @param duration The duration
   * @param timeUnit The time unit
   *
   * @return The low level message event containing adapted data read from the device if present
   */
  protected abstract Optional<MessageEvent> readFromDevice(int duration, TimeUnit timeUnit);

  /**
   * <p>Write a complete message buffer to the device.</p>
   *
   * @param buffer The buffer that will be written to the device
   *
   * @return The number of bytes written
   */
  protected abstract int writeToDevice(byte[] buffer);

}
