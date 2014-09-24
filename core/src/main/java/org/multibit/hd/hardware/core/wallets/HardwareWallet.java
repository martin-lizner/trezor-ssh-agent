package org.multibit.hd.hardware.core.wallets;

import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletException;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * <p>Interface to provide the following to applications:</p>
 * <ul>
 * <li>Low level communication methods to hardware wallet devices</li>
 * </ul>
 *
 * <p>Typically a hardware wallet device is wrapped by a client that provides the higher protocol messages such as
 * Initialise or Ping.</p>
 *
 * @since 0.0.1
 *  
 */
public interface HardwareWallet {

  /**
   * @return The hardware wallet specification in use
   */
  HardwareWalletSpecification getSpecification();

  /**
   * @return A default hardware wallet specification to use during the creation process if one is not supplied
   */
  HardwareWalletSpecification getDefaultSpecification();

  /**
   * <p>Apply any hardware wallet specific parameters</p>
   * <p>Implementers should override this, but call super.applySpecification(specification) as part of the application process</p>
   *
   * @param specification The {@link HardwareWalletSpecification}
   */
  void applySpecification(HardwareWalletSpecification specification);

  /**
   * <p>Perform any pre-connection initialisation</p>
   */
  void initialise();

  /**
   * <p>Attempt a connection to the device</p>
   *
   * <p>Implementers must ensure the following behaviour:</p>
   * <ul>
   *   <li>The device is assumed to be connected and discoverable</li>
   *   <li>Method will return false if USB HID librar</li>
   *   <li>A HardwareWalletSystemEvent.FAILURE event will be generated if the USB HID communication fails</li>
   * </ul>
   *
   * @return True if the connection was successful
   */
  boolean connect();

  /**
   * <p>Break the connection to the device</p>
   */
  void disconnect();

  /**
   * <p>Send a message to the device using the generated protocol buffer classes</p>
   * <p>Any response will be provided through the event bus subscribers</p>
   * <p>If this call fails the device will be closed and a DISCONNECT message will be emitted</p>
   *
   * @param message A generated protocol buffer message (e.g. Message.Initialize)
   */
  void sendMessage(Message message);

  /**
    * Parse a Trezor protobuf message from a data input stream
    * @param in The DataInputStream
    * @return the parsed Message
    */
   Message parseTrezorMessage(DataInputStream in) throws HardwareWalletException;

  /**
   * Get the DataInputStream that the hardware wallet is using to emit data
   */
   DataInputStream getDataInputStream();

  /**
    * Get the DataOutputStream that the hardware wallet is using to receive data
    */
   DataOutputStream getDataOutputStream();


}
