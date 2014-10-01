package org.multibit.hd.hardware.core.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Description of why the device requires a PIN matrix to be displayed</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class PinMatrixRequest implements HardwareWalletMessage {

  private final PinMatrixRequestType pinMatrixRequestType;

  public PinMatrixRequest(PinMatrixRequestType pinMatrixRequestType) {
    this.pinMatrixRequestType = pinMatrixRequestType;
  }

  /**
   * @return The button request type
   */
  public PinMatrixRequestType getPinMatrixRequestType() {
    return pinMatrixRequestType;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("pinMatrixRequestType", pinMatrixRequestType)
      .toString();
  }
}
