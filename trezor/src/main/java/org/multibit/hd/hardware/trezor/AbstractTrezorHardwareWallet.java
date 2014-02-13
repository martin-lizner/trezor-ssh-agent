package org.multibit.hd.hardware.trezor;

import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.ProtocolMessageType;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.multibit.hd.hardware.core.wallets.AbstractHardwareWallet;
import org.multibit.hd.hardware.trezor.protobuf.TrezorMessage;
import org.multibit.hd.hardware.trezor.protobuf.TrezorProtocolMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * <p>Abstract base class provide the following to Trezor hardware wallets:</p>
 * <ul>
 * <li>Access to common methods</li>
 * </ul>
 */
public abstract class AbstractTrezorHardwareWallet extends AbstractHardwareWallet<TrezorProtocolMessageType> {

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
  public void mapProtocolMessageTypeToDevice() {

    for (ProtocolMessageType protocolMessageType : ProtocolMessageType.values()) {

      // Using the same name greatly simplifies this process
      protocolMessageMap.put(
        TrezorProtocolMessageType.valueOf(protocolMessageType.name()),
        protocolMessageType
      );

    }

  }

  /**
   * <p>Blocking method to read from the data input stream and fire protocol message events</p>
   *
   * @param in The data input stream (must be open)
   *
   * @return True if the device is OK, false otherwise
   */
  public synchronized boolean adaptProtocolMessageToEvents(DataInputStream in) {

    // Very broad try-catch because a lot of things can go wrong here and need to be reported
    try {

      // Read and throw away the magic header markers
      in.readByte();
      in.readByte();

      // Read the header code and select a suitable parser
      final Short headerCode = in.readShort();
      final TrezorProtocolMessageType trezorMessageType = TrezorProtocolMessageType.getMessageTypeByHeaderCode(headerCode);

      Preconditions.checkNotNull(trezorMessageType, "'trezorMessageType' must be present");
      Preconditions.checkState(protocolMessageMap.containsKey(trezorMessageType), "Unmapped protocol message: {}", trezorMessageType.name());

      final ProtocolMessageType messageType = protocolMessageMap.get(trezorMessageType);

      // Read the detail length
      final int detailLength = in.readInt();

      // Read the remaining bytes
      final byte[] detail = new byte[detailLength];
      final int actualLength = in.read(detail, 0, detailLength);

      // Verify the read
      Preconditions.checkState(actualLength == detailLength, "Detail not read fully. Expected=" + detailLength + " Actual=" + actualLength);

      // Parse the detail into a message
      final Message message = TrezorProtocolMessageType.parse(headerCode, detail);
      log.debug("< {}", message.getClass().getName());

      if (TrezorProtocolMessageType.FAILURE.equals(trezorMessageType)) {
        log.error("FAILED: {}", ((TrezorMessage.Failure) message).getMessage());
      }

      // Fire the generic protocol event
      HardwareWalletEvents.fireProtocolEvent(messageType, message);

      // Must be OK to be here
      return true;

    } catch (EOFException e) {
      log.warn("Unexpected EOF from device");
      HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_EOF);
    } catch (IOException e) {
      log.warn("Unexpected disconnect from device.");
      HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_DISCONNECTED);
    } catch (Throwable e) {
      log.error("Unexpected error during read.", e);
      HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_FAILURE);
    }

    // Must have failed to be here
    return false;

  }

  @Override
  public synchronized void disconnect() {

    internalClose();
    hardwareWalletMonitorService.shutdownNow();

  }

  /**
   * <p>Implementations should handle their own shutdown before their threads are terminated</p>
   */
  public abstract void internalClose();

}
