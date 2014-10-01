package org.multibit.hd.hardware.core.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Description of why an operation failed</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class Failure implements HardwareWalletMessage {

  private final FailureType failureType;
  private final String failureMessage;

  public Failure(FailureType failureType, String failureMessage) {
    this.failureType = failureType;
    this.failureMessage = failureMessage;
  }

  /**
   * @return The failure type
   */
  public FailureType getFailureType() {
    return failureType;
  }

  /**
   * @return The failure message providing details
   */
  public String getFailureMessage() {
    return failureMessage;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("failureType", failureType)
      .append("failureMessage", failureMessage)
      .toString();
  }
}
