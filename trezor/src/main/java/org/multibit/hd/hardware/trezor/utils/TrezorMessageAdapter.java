package org.multibit.hd.hardware.trezor.utils;

import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;
import org.multibit.hd.hardware.core.messages.Failure;
import org.multibit.hd.hardware.core.messages.FailureType;
import org.multibit.hd.hardware.core.messages.Features;
import org.multibit.hd.hardware.core.messages.HardwareWalletMessage;

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
  public static Features adaptFeatures(TrezorMessage.Features source) {

    Features features = new Features();

    features.setBootloaderHash(source.getBootloaderHash().toByteArray());
    features.setBootloaderMode(source.getBootloaderMode());

    for (TrezorType.CoinType coinType : source.getCoinsList()) {
      features.getCoins().add(coinType.getCoinName());
    }

    features.setDeviceId(source.getDeviceId());
    features.setImported(source.getImported());
    features.setInitialized(source.isInitialized());

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
  public static HardwareWalletMessage adaptFailure(TrezorMessage.Failure source) {
    
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
  
}
