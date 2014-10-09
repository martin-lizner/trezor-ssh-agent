package org.multibit.hd.hardware.core.messages;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * <p>Value object to provide the following to high level messages:</p>
 * <ul>
 * <li>Transaction and signature serialisation after signing</li>
 * </ul>
 * <p>Signatures are provided on a per-input basis</p>
 *
 * @since 0.0.1
 *  
 */
public class TxRequestSerializedType {

  private final Optional<byte[]> serializedTx;
  private final Optional<Integer> signatureIndex;
  private final Optional<byte[]> signature;

  public TxRequestSerializedType(
    boolean hasSerializedTx,
    byte[] serializedTx,
    boolean hasSignatureIndex,
    int signatureIndex,
    boolean hasSignature,
    byte[] signature
  ) {

    // Drive presence/absence from the flags rather than content
    if (hasSerializedTx) {
      this.serializedTx = Optional.of(serializedTx);
    } else {
      this.serializedTx = Optional.absent();
    }
    if (hasSignatureIndex) {
      this.signatureIndex = Optional.of(signatureIndex);
    } else {
      this.signatureIndex = Optional.absent();
    }
    if (hasSignature) {
      this.signature = Optional.of(signature);
    } else {
      this.signature = Optional.absent();
    }
  }

  public Optional<byte[]> getSerializedTx() {
    return serializedTx;
  }

  public Optional<Integer> getSignatureIndex() {
    return signatureIndex;
  }

  public Optional<byte[]> getSignature() {
    return signature;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("serializedTx", serializedTx)
      .append("signatureIndex", signatureIndex)
      .append("signature", signature)
      .toString();
  }
}