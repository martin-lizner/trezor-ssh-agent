package org.multibit.hd.hardware.core.messages;

import com.google.common.base.Optional;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Carries result of sign identity operation</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class SignedIdentity implements HardwareWalletMessage {

  private final Optional<byte[]> addressBytes;
  private final Optional<byte[]> publicKeyBytes;
  private final Optional<byte[]> signatureBytes;

  public SignedIdentity(boolean hasAddress, byte[] addressBytes, boolean hasPublicKey, byte[] publicKeyBytes, boolean hasSignature, byte[] signatureBytes) {

    // Drive presence/absence from the flags rather than content
    if (hasAddress) {
      this.addressBytes = Optional.of(addressBytes);
    } else {
      this.addressBytes = Optional.absent();
    }
    if (hasPublicKey) {
      this.publicKeyBytes = Optional.of(publicKeyBytes);
    } else {
      this.publicKeyBytes = Optional.absent();
    }
    if (hasSignature) {
      this.signatureBytes = Optional.of(signatureBytes);
    } else {
      this.signatureBytes = Optional.absent();
    }
  }

  public Optional<byte[]> getAddressBytes() {
    return addressBytes;
  }

  public Optional<byte[]> getPublicKeyBytes() {
    return publicKeyBytes;
  }

  public Optional<byte[]> getSignatureBytes() {
    return signatureBytes;
  }
}
