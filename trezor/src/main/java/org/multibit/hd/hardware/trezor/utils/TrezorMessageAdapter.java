package org.multibit.hd.hardware.trezor.utils;

import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;
import org.multibit.hd.hardware.core.messages.*;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Adapter to provide the following to Trezor messages:</p>
 * <ul>
 * <li>Adaption from a Trezor message to a Core message</li>
 * </ul>
 *
 * <p>The Core messages are used in the high level hardware wallet events
 * that are passed to downstream consumer applications and are device agnostic.</p>
 *
 * @since 0.0.1
 *  
 */
public class TrezorMessageAdapter {

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static Success adaptSuccess(TrezorMessage.Success source) {

    return new Success(source.getMessage());

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static Features adaptFeatures(TrezorMessage.Features source) {

    Features features = new Features();

    features.setBootloaderHash(source.getBootloaderHash().toByteArray());
    features.setBootloaderMode(source.getBootloaderMode());

    for (TrezorType.CoinType coinType : source.getCoinsList()) {
      features.getCoins().add(coinType.getCoinName());
    }

    features.setDeviceId(source.getDeviceId());
    features.setImported(source.getImported());
    features.setInitialized(source.getInitialized());

    features.setLabel(source.getLabel());
    features.setLanguage(source.getLanguage());

    features.setPassphraseProtection(source.getPassphraseProtection());
    features.setPinProtection(source.getPinProtection());

    features.setRevision(source.getRevision().toByteArray());

    features.setVersion(
      source.getMajorVersion() + "." +
        source.getMinorVersion() + "." +
        source.getPatchVersion()
    );
    features.setVendor(source.getVendor());

    return features;

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static Failure adaptFailure(TrezorMessage.Failure source) {

    FailureType failureType = adaptFailureType(source.getCode());

    return new Failure(failureType, source.getMessage());

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static FailureType adaptFailureType(TrezorType.FailureType source) {

    switch (source) {
      case Failure_ActionCancelled:
        return FailureType.ACTION_CANCELLED;
      case Failure_ButtonExpected:
        return FailureType.BUTTON_EXPECTED;
      case Failure_FirmwareError:
        return FailureType.FIRMWARE_ERROR;
      case Failure_InvalidSignature:
        return FailureType.INVALID_SIGNATURE;
      case Failure_NotEnoughFunds:
        return FailureType.NOT_ENOUGH_FUNDS;
      case Failure_NotInitialized:
        return FailureType.NOT_INITIALIZED;
      case Failure_Other:
        return FailureType.OTHER;
      case Failure_PinCancelled:
        return FailureType.PIN_CANCELLED;
      case Failure_PinExpected:
        return FailureType.PIN_EXPECTED;
      case Failure_PinInvalid:
        return FailureType.PIN_INVALID;
      case Failure_SyntaxError:
        return FailureType.SYNTAX_ERROR;
      case Failure_UnexpectedMessage:
        return FailureType.UNEXPECTED_MESSAGE;
      default:
        return null;
    }

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static ButtonRequest adaptButtonRequest(TrezorMessage.ButtonRequest source) {

    ButtonRequestType buttonRequestType = adaptButtonRequestType(source.getCode());

    return new ButtonRequest(buttonRequestType, source.getData());

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static ButtonRequestType adaptButtonRequestType(TrezorType.ButtonRequestType source) {

    switch (source) {
      case ButtonRequest_Address:
        return ButtonRequestType.ADDRESS;
      case ButtonRequest_ConfirmOutput:
        return ButtonRequestType.CONFIRM_OUTPUT;
      case ButtonRequest_ConfirmWord:
        return ButtonRequestType.CONFIRM_WORD;
      case ButtonRequest_FeeOverThreshold:
        return ButtonRequestType.FEE_OVER_THRESHOLD;
      case ButtonRequest_FirmwareCheck:
        return ButtonRequestType.FIRMWARE_CHECK;
      case ButtonRequest_Other:
        return ButtonRequestType.OTHER;
      case ButtonRequest_ProtectCall:
        return ButtonRequestType.PROTECT_CALL;
      case ButtonRequest_ResetDevice:
        return ButtonRequestType.RESET_DEVICE;
      case ButtonRequest_SignTx:
        return ButtonRequestType.SIGN_TX;
      case ButtonRequest_WipeDevice:
        return ButtonRequestType.WIPE_DEVICE;
      default:
        return null;
    }

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static PinMatrixRequest adaptPinMatrixRequest(TrezorMessage.PinMatrixRequest source) {

    PinMatrixRequestType pinMatrixRequestType = adaptPinMatrixRequestType(source.getType());

    return new PinMatrixRequest(pinMatrixRequestType);

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static PinMatrixRequestType adaptPinMatrixRequestType(TrezorType.PinMatrixRequestType source) {

    switch (source) {
      case PinMatrixRequestType_Current:
        return PinMatrixRequestType.CURRENT;
      case PinMatrixRequestType_NewFirst:
        return PinMatrixRequestType.NEW_FIRST;
      case PinMatrixRequestType_NewSecond:
        return PinMatrixRequestType.NEW_SECOND;
      default:
        return null;
    }

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static MainNetAddress adaptAddress(TrezorMessage.Address source) {

    return new MainNetAddress(source.getAddress());

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static PublicKey adaptPublicKey(TrezorMessage.PublicKey source) {

    HDNodeType hdNodeType = null;

    if (source.hasNode()) {
      hdNodeType = adaptHDNodeType(source.getNode());
      if (!source.hasXpub()) {
        // Decode the node information into an xpub

        ByteBuffer ser = ByteBuffer.allocate(78);
        ser.putInt(0x0488b21e); // Version (results in "xpub" on MainNet)
        ser.put(hdNodeType.getDepth().get().byteValue()); // Depth
        ser.putInt(hdNodeType.getFingerprint().get()); // Fingerprint (0x00000000 for master)
        ser.putInt(hdNodeType.getChildNum().get()); // Child number (0x00000000 for master)
        ser.put(hdNodeType.getChainCode().get());// The chain code (32 bytes)
        ser.put(hdNodeType.getPublicKey().get());// The public key (33 bytes)

        checkState(ser.position() == 78);

        byte[] input = ser.array();
        int inputLength = input.length;
        byte[] checksummed = new byte[inputLength + 4];
        System.arraycopy(input, 0, checksummed, 0, inputLength);
        byte[] checksum = Sha256Hash.hashTwice(input);
        System.arraycopy(checksum, 0, checksummed, inputLength, 4);

        String xpub = Base58.encode(checksummed);

        return new PublicKey(
          true,
          xpub,
          checksummed,
          source.hasNode(),
          hdNodeType
        );

      }
    }

    return new PublicKey(
      source.hasXpub(),
      source.getXpub(),
      source.getXpubBytes().toByteArray(),
      source.hasNode(),
      hdNodeType
    );

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static HDNodeType adaptHDNodeType(TrezorType.HDNodeType source) {

    return new HDNodeType(
      source.hasPublicKey(),
      source.getPublicKey().toByteArray(),
      source.hasPrivateKey(),
      source.getPrivateKey().toByteArray(),
      source.hasChainCode(),
      source.getChainCode().toByteArray(),
      source.hasChildNum(),
      source.getChildNum(),
      source.hasDepth(),
      source.getDepth(),
      source.hasFingerprint(),
      source.getFingerprint()
    );

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static TxRequest adaptTxRequest(TrezorMessage.TxRequest source) {

    TxRequestType txRequestType = adaptTxRequestType(source.getRequestType());
    TxRequestDetailsType txRequestDetailsType = adaptTxRequestDetailsType(source.getDetails());
    TxRequestSerializedType txRequestSerializedType = adaptTxRequestSerializedType(source.getSerialized());

    return new TxRequest(
      txRequestType,
      txRequestDetailsType,
      txRequestSerializedType
    );

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  private static TxRequestType adaptTxRequestType(TrezorType.RequestType source) {

    switch (source.getNumber()) {
      case 0:
        return TxRequestType.TX_INPUT;
      case 1:
        return TxRequestType.TX_OUTPUT;
      case 2:
        return TxRequestType.TX_META;
      case 3:
        return TxRequestType.TX_FINISHED;
      default:
        return null;
    }
  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  private static TxRequestDetailsType adaptTxRequestDetailsType(TrezorType.TxRequestDetailsType source) {
    return new TxRequestDetailsType(
      source.hasRequestIndex(),
      source.getRequestIndex(),
      source.hasTxHash(),
      source.getTxHash().toByteArray()
    );
  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  private static TxRequestSerializedType adaptTxRequestSerializedType(TrezorType.TxRequestSerializedType source) {
    return new TxRequestSerializedType(
      source.hasSerializedTx(),
      source.getSerializedTx().toByteArray(),
      source.hasSignatureIndex(),
      source.getSignatureIndex(),
      source.hasSignature(),
      source.getSignature().toByteArray()
    );
  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static MessageSignature adaptMessageSignature(TrezorMessage.MessageSignature source) {
    return new MessageSignature(
      source.getAddress(),
      source.getSignature().toByteArray()
    );

  }

  /**
   * @param source The source message
   *
   * @return The adapted Core message
   */
  public static CipheredKeyValue adaptCipheredKeyValue(TrezorMessage.CipheredKeyValue source) {
    return new CipheredKeyValue(
      source.hasValue(),
      source.getValue().toByteArray()
    );
  }
}
