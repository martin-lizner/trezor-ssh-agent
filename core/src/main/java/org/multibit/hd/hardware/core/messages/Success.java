package org.multibit.hd.hardware.core.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

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

  /**
   * @param message The message
   */
  public Success(String message) {
    this.message = message;
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
}
