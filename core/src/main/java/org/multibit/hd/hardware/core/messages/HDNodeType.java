package org.multibit.hd.hardware.core.messages;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Wrapper for a HD node type</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class HDNodeType implements HardwareWalletMessage {

  private final Optional<byte[]> publicKey;
  private final Optional<byte[]> privateKey;
  private final Optional<byte[]> chainCode;
  private final Optional<Integer> childNum;
  private final Optional<Integer> depth;
  private final Optional<Integer> fingerprint;

  public HDNodeType(
    boolean hasPublicKey,
    byte[] publicKey,
    boolean hasPrivateKey,
    byte[] privateKey,
    boolean hasChainCode,
    byte[] chainCode,
    boolean hasChildNum,
    int childNum,
    boolean hasDepth,
    int depth,
    boolean hasFingerprint,
    int fingerprint
  ) {

    // Drive presence/absence from the flags rather than content
    if (hasPublicKey) {
      this.publicKey = Optional.of(publicKey);
    } else {
      this.publicKey = Optional.absent();
    }
    if (hasPrivateKey) {
      this.privateKey = Optional.of(privateKey);
    } else {
      this.privateKey = Optional.absent();
    }

    if (hasChainCode) {
      this.chainCode = Optional.of(chainCode);
    } else {
      this.chainCode = Optional.absent();
    }
    if (hasChildNum) {
      this.childNum = Optional.of(childNum);
    } else {
      this.childNum = Optional.absent();
    }

    if (hasDepth) {
      this.depth = Optional.of(depth);
    } else {
      this.depth = Optional.absent();
    }
    if (hasFingerprint) {
      this.fingerprint = Optional.of(fingerprint);
    } else {
      this.fingerprint = Optional.absent();
    }

  }

  public Optional<byte[]> getPublicKey() {
    return publicKey;
  }

  public Optional<byte[]> getPrivateKey() {
    return privateKey;
  }

  public Optional<byte[]> getChainCode() {
    return chainCode;
  }

  public Optional<Integer> getChildNum() {
    return childNum;
  }

  public Optional<Integer> getDepth() {
    return depth;
  }

  public Optional<Integer> getFingerprint() {
    return fingerprint;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("publicKey", "****") // Avoid logging this information
      .append("privateKey", "****") // Avoid logging this information
      .append("chainCode", "****") // Avoid logging this information
      .append("childNum", childNum)
      .append("depth", depth)
      .append("fingerprint", fingerprint)
      .toString();
  }
}
