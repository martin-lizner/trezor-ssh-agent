package org.multibit.hd.hardware.core.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;

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

  private final String message;
  private final byte[] payload;

  /**
   * @param message The message
   * @param payload The payload
   */
  public CipheredKeyValue(String message, byte[] payload) {
    this.message = message;
    this.payload = Arrays.copyOf(payload, payload.length);
  }

  /**
   * @return The message
   */
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("message", message)
      .toString();
  }

  /**
   * @return The cipher key payload
   */
  public byte[] getPayload() {
    return Arrays.copyOf(payload, payload.length);
  }

}
