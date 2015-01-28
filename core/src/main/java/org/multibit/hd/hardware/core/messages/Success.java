package org.multibit.hd.hardware.core.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Indication of successful completion of operation</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class Success implements HardwareWalletMessage {

  private final String message;
  private final byte[] payload;

  /**
   * @param message The message
   * @param payload The payload (e.g. a cipher key value)
   */
  public Success(String message, byte[] payload) {
    this.message = message;
    this.payload = Arrays.copyOf(payload, payload.length);
  }

  /**
   * @return The message
   */
  public String getMessage() {
    return message;
  }

  /**
   * @return The payload
   */
  public byte[] getPayload() {
    return Arrays.copyOf(payload, payload.length);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("message", message)
      .append("payload", "****") // Payload allows a replay attack
      .toString();
  }
}
