package org.multibit.hd.hardware.core.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Description of why the device requires a button press</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class ButtonRequest implements HardwareWalletMessage {

  private final ButtonRequestType buttonRequestType;
  private final String buttonMessage;

  public ButtonRequest(ButtonRequestType buttonRequestType, String buttonMessage) {
    this.buttonRequestType = buttonRequestType;
    this.buttonMessage = buttonMessage;
  }

  /**
   * @return The button request type
   */
  public ButtonRequestType getButtonRequestType() {
    return buttonRequestType;
  }

  /**
   * @return The button message providing details
   */
  public String getButtonMessage() {
    return buttonMessage;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("buttonRequestType", buttonRequestType)
      .append("buttonMessage", buttonMessage)
      .toString();
  }
}
