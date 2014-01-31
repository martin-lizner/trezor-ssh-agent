package org.multibit.hd.hardware.core.events;

import org.multibit.hd.hardware.core.messages.SystemMessageType;

/**
 * <p>Event to provide the following to application API:</p>
 * <ul>
 * <li>Notification of a hardware wallet event at the system layer</li>
 * </ul>
 * <p>System messages provide the state of the connection to the hardware wallet</p>
 *
 * @since 0.0.1
 *  
 */
public class HardwareWalletSystemEvent {

  private final SystemMessageType messageType;

  /**
   * @param messageType The protocol message type
   */
  public HardwareWalletSystemEvent(SystemMessageType messageType) {

    this.messageType = messageType;
  }

  /**
   * @return The system message type
   */
  public SystemMessageType getMessageType() {
    return messageType;
  }

}
