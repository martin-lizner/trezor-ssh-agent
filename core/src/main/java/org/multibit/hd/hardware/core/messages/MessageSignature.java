package org.multibit.hd.hardware.core.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Details for a signed message</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class MessageSignature implements HardwareWalletMessage {

  private final String address;
  private final byte[] signature;

  public MessageSignature(String address, byte[] signature) {

    this.address = address;
    this.signature = Arrays.copyOf(signature, signature.length);

  }

  /**
   * @return The address used for signing
   */
  public String getAddress() {
    return address;
  }

  /**
   * @return The signature
   */
  public byte[] getSignature() {
    return Arrays.copyOf(signature, signature.length);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("address", address)
      .append("signature", signature)
      .toString();
  }
}
