package org.multibit.hd.hardware.core.events;

/**
 * <p>Signature interface to provide the following to Hardware Wallet Event API:</p>
 * <ul>
 * <li>Identification of hardware wallet events</li>
 * </ul>
 * <p>A hardware wallet event should be named using a noun as the first part of the name (e.g. ProtocolEvent)</p>
 * <p>A hardware wallet event can occur at any time and will not be synchronized with other events.</p>
 *
 * @since 0.0.1
 *         
 */
public interface HardwareWalletEvent {
}
