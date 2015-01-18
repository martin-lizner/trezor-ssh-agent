package org.multibit.hd.hardware.core.messages;

import com.google.common.base.Optional;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Wrapper for a public key</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class PublicKey implements HardwareWalletMessage {

  private final Optional<String> xpub;
  private final Optional<byte[]> xpubBytes;
  private final Optional<HDNodeType> hdNodeType;

  public PublicKey(
    boolean hasXpub,
    String xpub,
    byte[] xpubBytes,
    boolean hasNode,
    HDNodeType hdNodeType
  ) {

    // Drive presence/absence from the flags rather than content
    if (hasXpub) {
      this.xpub = Optional.of(xpub);
      this.xpubBytes = Optional.of(xpubBytes);
    } else {
      this.xpub = Optional.absent();
      this.xpubBytes = Optional.absent();
    }

    if (hasNode) {
      this.hdNodeType = Optional.of(hdNodeType);
    } else {
      this.hdNodeType = Optional.absent();
    }
  }

  public Optional<String> getXpub() {
    return xpub;
  }

  public Optional<byte[]> getXpubBytes() {
    return xpubBytes;
  }

  public Optional<HDNodeType> getHdNodeType() {
    return hdNodeType;
  }

}
