package org.multibit.hd.hardware.core.wallets;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.MessageEvent;

import java.util.concurrent.TimeUnit;

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
public interface HardwareWallet extends Connectable {

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
   * <p>Read a protobuf message from the hardware wallet and adapt it to a Core message event.</p>
   * <p>Consumers of this interface should check the returned message event wrapper for failure modes
   * as appropriate.</p>
   *
   * <p>Forcing early adaption to a message event eases the implementation in many ways.</p>
   *
   * @return The low level message event wrapping the adapted protobuf message read from the hardware wallet if present
   * @param duration
   * @param timeUnit
   */
  public Optional<MessageEvent> readMessage(int duration, TimeUnit timeUnit);

  /**
   * <p>Send a message to the hardware wallet using the generated protocol buffer classes</p>
   * <p>Any response will be provided through the event bus subscribers</p>
   * <p>If this call fails the hardware wallet will be closed and a DISCONNECT message will be emitted</p>
   *
   * @param message A generated protocol buffer message (e.g. Message.Initialize)
   */
  public void writeMessage(Message message);
}
