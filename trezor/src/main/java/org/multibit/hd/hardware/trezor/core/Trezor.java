package org.multibit.hd.hardware.trezor.core;

import com.google.protobuf.Message;

/**
 * <p>Interface to provide the following to applications:</p>
 * <ul>
 * <li>Common methods available to different Trezor devices</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public interface Trezor {

  /**
   * <p>Attempt a connection to the device</p>
   */
  void connect();

  /**
   * <p>Break the connection to the device</p>
   */
  void close();

  /**
   * <p>Send a message to the device using the generated protocol buffer classes</p>
   * <p>Any response will be provided through the listener interface (Callback mode)</p>
   * <p>If this call fails the device will be closed and a DISCONNECT message will be emitted</p>
   *
   * @param message A generated protocol buffer message (e.g. Message.Initialize)
   */
  void sendMessage(Message message);

  /**
   * <p>Add a Trezor listener - duplicates will be rejected</p>
   *
   * @param trezorListener A Trezor listener
   */
  void addListener(TrezorListener trezorListener);

  /**
   * <p>Remove a Trezor listener - </p>
   *
   * @param trezorListener A Trezor listener
   */
  void removeListener(TrezorListener trezorListener);

}
