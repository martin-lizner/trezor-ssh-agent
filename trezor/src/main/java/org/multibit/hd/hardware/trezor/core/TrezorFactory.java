package org.multibit.hd.hardware.trezor.core;

import com.google.common.base.Optional;
import org.multibit.hd.hardware.trezor.core.trezors.SocketTrezor;
import org.multibit.hd.hardware.trezor.core.trezors.UsbTrezor;

/**
 * <p>Factory to provide the following to applications:</p>
 * <ul>
 * <li>Access to Trezor devices over different communication links</li>
 * </ul>
 * <p>Example:</p>
 * <pre>
 *   SocketTrezor trezor = TrezorFactory.INSTANCE.newSocketTrezor("192.168.0.1",3000)
 * </pre>
 * <p>See the trezorj-org.multibit.hd.hardware.trezor.examples module for more comprehensive org.multibit.hd.hardware.trezor.examples</p>
 *
 * @since 0.0.1
 *         
 */
public class TrezorFactory {

  /**
   * Utilities do not require a public constructor
   */
  private TrezorFactory() {
  }

  /**
   * <p>Create a new instance of a USB-based Trezor device (standard)</p>
   * @param vendorIdOptional The vendor ID (default is 0x10c4)
   * @param productIdOptional The product ID (default is 0xea80)
   * @param serialNumberOptional The device serial number (default is to accept any)
   * @return A USB-based Trezor
   */
  public static UsbTrezor newUsbTrezor(Optional<Integer> vendorIdOptional, Optional<Integer> productIdOptional, Optional<String> serialNumberOptional) {

    return new UsbTrezor(vendorIdOptional, productIdOptional, serialNumberOptional);
  }

  /**
   * <p>Create a new instance of a socket-based Trezor device (development)</p>
   *
   * @param host The host  (e.g. "localhost", "192.168.0.1" etc)
   * @param port The port (e.g. 3000)
   * @return A socket-based Trezor
   */
  public static SocketTrezor newSocketTrezor(String host, int port) {

    return new SocketTrezor(host, port);

  }
}
