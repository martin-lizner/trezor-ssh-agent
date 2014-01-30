package org.multibit.hd.hardware.core.events;

/**
 * <p>Event to provide the following to application API:</p>
 * <ul>
 * <li>Notification of a hardware event at the protocol layer</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class HardwareEvent {

  private final HardwareEventType eventType;

  /**
   * @param eventType The event type
   */
  public HardwareEvent(HardwareEventType eventType) {
    this.eventType = eventType;
  }

  public HardwareEventType getEventType() {
    return eventType;
  }
}
