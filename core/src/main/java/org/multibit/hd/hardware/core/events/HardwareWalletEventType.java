package org.multibit.hd.hardware.core.events;

/**
 * <p>Enum to provide the following to HardwareWallet events:</p>
 * <ul>
 * <li>Provision of event types to allow easy triage in consuming applications</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public enum HardwareWalletEventType {

  /**
   * Indicates that this is a protocol message
   */
  PROTOCOL_MESSAGE,

  /**
   * Device encountered an error not associated with I/O (e.g. thread interrupt)
   */
  DEVICE_FAILURE,

  /**
   * Received EOF from device (no data in receive buffer after timeout when some is expected)
   */
  DEVICE_EOF,

  /**
   * Received on a device connect (communications established at the wire level)
   */
  DEVICE_CONNECTED,

  /**
   * Received on a device disconnect (no longer able to communicate)
   */
  DEVICE_DISCONNECTED,

  // End of enum
  ;

}
