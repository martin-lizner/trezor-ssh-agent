package org.multibit.hd.hardware.core.wallets;

import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;

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
   *
   * <p>Typically this would involve initialising native libraries and verifying their communications</p>
   *
   * @return True if the native libraries initialised successfully
   */
  boolean initialise();

  /**
   * <p>Attempt a connection to the device</p>
   *
   * <p>Implementers must ensure the following behaviour:</p>
   * <ul>
   * <li>The device is assumed to be connected and discoverable</li>
   * <li>Method will return false if the no matching device is found</li>
   * <li>A DEVICE_FAILED event will be generated if subsequent USB HID communication fails (i.e. device is broken)</li>
   * </ul>
   *
   * @return True if the connection was successful, false if a failure is permanent (environment failure)
   */
  boolean connect();

  /**
   * <p>Break the connection to the device</p>
   */
  void disconnect();

  /**
   * <p>Read a protobuf message from the hardware wallet</p>
   * <p>If this call fails the hardware wallet will be closed and a DISCONNECT message will be emitted</p>
   *
   * @return The protobuf message read from the hardware wallet
   */
  public abstract Message readMessage();

  /**
   * <p>Send a message to the hardware wallet using the generated protocol buffer classes</p>
   * <p>Any response will be provided through the event bus subscribers</p>
   * <p>If this call fails the hardware wallet will be closed and a DISCONNECT message will be emitted</p>
   *
   * @param message A generated protocol buffer message (e.g. Message.Initialize)
   */
  public void writeMessage(Message message);
}
