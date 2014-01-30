package org.multibit.hd.hardware.core.events;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Factory to provide the following to application API:</p>
 * <ul>
 * <li>Entry point to broadcast org.multibit.hd.hardware.trezor.core events</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class HardwareEvents {

  private static final Logger log = LoggerFactory.getLogger(HardwareEvents.class);

  private static final EventBus hardwareEventBus = new EventBus();

  /**
   * Utilities have a private constructor
   */
  private HardwareEvents() {
  }

  /**
   * <p>Broadcast a new "hardware wallet " event</p>
   */
  public static void fireInitializeEvent() {

    log.debug("Firing 'initialize' event");
    hardwareEventBus.post(new HardwareEvent(null));

  }

  /*
  Initialize

  Ping

  Success
  Failure

  GetUUID
  UUID

  OtpRequest
  OtpAck
  OtpCancel

  GetEntropy
  Entropy

  GetMasterPublicKey
  MasterPublicKey

  LoadDevice
  ResetDevice

  SignTx
  Features

  // PIN
  PinRequest
  PinAck
  PinCancel

  // Transactions
  TxRequest
  TxInput
  TxOutput
  SetMaxFeeKb

  // Buttons
  ButtonRequest
  ButtonAck
  ButtonCancel

  // Address
  GetAddress
  Address

  // Debugging messages
  DebugLinkDecision
  DebugLinkGetState
  DebugLinkState
  DebugLinkStop
*/

}
