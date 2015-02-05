package org.multibit.hd.hardware.core.messages;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Carries result of cipher key operation</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class CipheredKeyValue implements HardwareWalletMessage {

  private final Optional<byte[]> payload;

  /**
   * @param hasPayload True if the payload is present
   * @param payload    The payload
   */
  public CipheredKeyValue(boolean hasPayload, byte[] payload) {

    // Drive presence/absence from the flags rather than content
    if (hasPayload) {
      this.payload = Optional.of(payload);
    } else {
      this.payload = Optional.absent();
    }
  }

  /**
   * @return The payload
   */
  public Optional<byte[]> getPayload() {
    return payload;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("payload", payload)
      .toString();
  }

}
