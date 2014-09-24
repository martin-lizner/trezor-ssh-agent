package org.multibit.hd.hardware.trezor;

import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.multibit.hd.hardware.core.HardwareWalletException;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.messages.ProtocolMessageType;
import org.multibit.hd.hardware.core.wallets.AbstractHardwareWallet;
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
  public void mapProtocolMessageTypeToDevice() {

    // Ensure all protocol message types are covered
    for (ProtocolMessageType protocolMessageType : ProtocolMessageType.values()) {

      final TrezorMessage.MessageType trezorMessageType;

      switch (protocolMessageType) {

        case INITALIZE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_Initialize;
          break;
        case PING:
          trezorMessageType = TrezorMessage.MessageType.MessageType_Ping;
          break;
        case SUCCESS:
          trezorMessageType = TrezorMessage.MessageType.MessageType_Success;
          break;
        case FAILURE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_Failure;
          break;
        case CHANGE_PIN:
          trezorMessageType = TrezorMessage.MessageType.MessageType_ChangePin;
          break;
        case WIPE_DEVICE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_WipeDevice;
          break;
        case FIRMWARE_ERASE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_FirmwareErase;
          break;
        case FIRMWARE_UPLOAD:
          trezorMessageType = TrezorMessage.MessageType.MessageType_FirmwareUpload;
          break;
        case GET_ENTROPY:
          trezorMessageType = TrezorMessage.MessageType.MessageType_GetEntropy;
          break;
        case ENTROPY:
          trezorMessageType = TrezorMessage.MessageType.MessageType_Entropy;
          break;
        case GET_PUBLIC_KEY:
          trezorMessageType = TrezorMessage.MessageType.MessageType_GetPublicKey;
          break;
        case PUBLIC_KEY:
          trezorMessageType = TrezorMessage.MessageType.MessageType_PublicKey;
          break;
        case LOAD_DEVICE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_LoadDevice;
          break;
        case RESET_DEVICE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_ResetDevice;
          break;
        case SIGN_TX:
          trezorMessageType = TrezorMessage.MessageType.MessageType_SignTx;
          break;
        case SIMPLE_SIGN_TX:
          trezorMessageType = TrezorMessage.MessageType.MessageType_SimpleSignTx;
          break;
        case FEATURES:
          trezorMessageType = TrezorMessage.MessageType.MessageType_Features;
          break;
        case PIN_MATRIX_REQUEST:
          trezorMessageType = TrezorMessage.MessageType.MessageType_PinMatrixRequest;
          break;
        case PIN_MATRIX_ACK:
          trezorMessageType = TrezorMessage.MessageType.MessageType_PinMatrixAck;
          break;
        case CANCEL:
          trezorMessageType = TrezorMessage.MessageType.MessageType_Cancel;
          break;
        case TX_REQUEST:
          trezorMessageType = TrezorMessage.MessageType.MessageType_TxRequest;
          break;
        case TX_ACK:
          trezorMessageType = TrezorMessage.MessageType.MessageType_TxAck;
          break;
        case CIPHER_KEY_VALUE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_CipherKeyValue;
          break;
        case CLEAR_SESSION:
          trezorMessageType = TrezorMessage.MessageType.MessageType_ClearSession;
          break;
        case APPLY_SETTINGS:
          trezorMessageType = TrezorMessage.MessageType.MessageType_ApplySettings;
          break;
        case BUTTON_REQUEST:
          trezorMessageType = TrezorMessage.MessageType.MessageType_ButtonRequest;
          break;
        case BUTTON_ACK:
          trezorMessageType = TrezorMessage.MessageType.MessageType_ButtonAck;
          break;
        case GET_ADDRESS:
          trezorMessageType = TrezorMessage.MessageType.MessageType_GetAddress;
          break;
        case ADDRESS:
          trezorMessageType = TrezorMessage.MessageType.MessageType_Address;
          break;
        case ENTROPY_REQUEST:
          trezorMessageType = TrezorMessage.MessageType.MessageType_EntropyRequest;
          break;
        case ENTROPY_ACK:
          trezorMessageType = TrezorMessage.MessageType.MessageType_EntropyAck;
          break;
        case SIGN_MESSAGE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_SignMessage;
          break;
        case VERIFY_MESSAGE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_VerifyMessage;
          break;
        case MESSAGE_SIGNATURE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_MessageSignature;
          break;
        case ENCRYPT_MESSAGE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_EncryptMessage;
          break;
        case DECRYPT_MESSAGE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_DecryptMessage;
          break;
        case PASSPHRASE_REQUEST:
          trezorMessageType = TrezorMessage.MessageType.MessageType_PassphraseRequest;
          break;
        case PASSPHRASE_ACK:
          trezorMessageType = TrezorMessage.MessageType.MessageType_PassphraseAck;
          break;
        case ESTIMATE_TX_SIZE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_EstimateTxSize;
          break;
        case TX_SIZE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_TxSize;
          break;
        case RECOVERY_DEVICE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_RecoveryDevice;
          break;
        case WORD_REQUEST:
          trezorMessageType = TrezorMessage.MessageType.MessageType_WordRequest;
          break;
        case WORD_ACK:
          trezorMessageType = TrezorMessage.MessageType.MessageType_WordAck;
          break;
        case DEBUG_LINK_DECISION:
          trezorMessageType = TrezorMessage.MessageType.MessageType_DebugLinkDecision;
          break;
        case DEBUG_LINK_GET_STATE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_DebugLinkGetState;
          break;
        case DEBUG_LINK_STATE:
          trezorMessageType = TrezorMessage.MessageType.MessageType_DebugLinkState;
          break;
        case DEBUG_LINK_STOP:
          trezorMessageType = TrezorMessage.MessageType.MessageType_DebugLinkStop;
          break;
        case DEBUG_LINK_LOG:
          trezorMessageType = TrezorMessage.MessageType.MessageType_DebugLinkLog;
          break;
        default:
          throw new IllegalStateException("Missing protocol message type: " + protocolMessageType.name());

      }

      // Using the same name greatly simplifies this process
      protocolMessageMap.put(
        trezorMessageType,
        protocolMessageType
      );

    }

  }

  /**
   * Parse a Trezor protobuf message from a data input stream
   * @param in The DataInputStream
   * @return the parsed Message
   */
  @Override
  public synchronized Message parseTrezorMessage(DataInputStream in) throws HardwareWalletException {
    // Very broad try-catch because a lot of things can go wrong here and need to be reported
    try {

      // Read and throw away the magic header markers
      in.readByte();
      in.readByte();

      // Read the header code and select a suitable parser
      final Short headerCode = in.readShort();
      final TrezorMessage.MessageType trezorMessageType = TrezorMessageUtils.getMessageTypeByHeaderCode(headerCode);

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
      final Message message = TrezorMessageUtils.parse(headerCode, detail);
      log.debug("< {}", message.getClass().getName());
      return message;
    } catch (EOFException e) {
      throw new HardwareWalletException("Unexpected EOF from device", e);
    } catch (IOException e) {
      throw new HardwareWalletException("Unexpected disconnect from device", e);
    } catch (Throwable e) {
      throw new HardwareWalletException("Unexpected error during read", e);
    }
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
