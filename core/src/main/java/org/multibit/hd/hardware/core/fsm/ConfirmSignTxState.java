package org.multibit.hd.hardware.core.fsm;

import org.bitcoinj.core.Transaction;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletEventType;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.messages.TxRequest;
import org.multibit.hd.hardware.core.messages.TxRequestSerializedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>State to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>State transitions based on low level message events</li>
 * </ul>
 * <p>The "confirm sign transaction" state occurs in response to a SIGN_TX message and handles
 * the ongoing button requests, success and failure messages coming from the device as it
 * signs the transaction presented.</p>
 *
 * <p>A typical sequence for signing a transaction would be as follows:</p>
 * <p>hash 0 is current, prev hash 0 is hash of parent to input 0</p>
 * <p>addressN is the co-ordinates of a receiving address (e.g. [0,1] as chain, address index)</p>
 * <ol>
 *   <li>> SIGN_TX (input count, output count)</li>
 *   <li>< PIN_MATRIX_REQUEST </li>
 *   <li>> PIN_ACK (pin)</li>
 *   <li>< TX_REQUEST (current input 0)</li>
 *   <li>> TX_ACK (input 0: addressN, prev hash, prev index, script type</li>
 *   <li>< TX_REQUEST (meta, prev hash 0)</li>
 *   <li>> TX_ACK (prev hash 0: lock time, input count, output count)</li>
 *   <li>< TX_REQUEST (hash 0, input 0)</li>
 *   <li>> TX_ACK (hash 0: input 0: addressN, prev hash, prev index, script type</li>
 *   <li>< TX_REQUEST (request hash 0, output 0)</li>
 *   <li>> TX_ACK (hash 0: output 0: address, amount, script type) </li>
 *   <li>< TX_REQUEST (hash 0: output 1)</li>
 *   <li>> TX_ACK (hash 0: output 1: address, amount, script type)</li>
 * </ol>
 * @since 0.0.1
 *  
 */
public class ConfirmSignTxState extends AbstractHardwareWalletState {

  private static final Logger log = LoggerFactory.getLogger(ConfirmSignTxState.class);

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      case PIN_MATRIX_REQUEST:
        // Device is asking for a PIN matrix to be displayed (user must read the screen carefully)
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_PIN_ENTRY, event.getMessage().get());
        // Further state transitions will occur after the user has provided the PIN via the service
        break;
      case PASSPHRASE_REQUEST:
        // Device is asking for a passphrase screen to be displayed
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_PASSPHRASE_ENTRY);
        // Further state transitions will occur after the user has provided the passphrase via the service
        break;
      case TX_REQUEST:
        // Device is requesting a transaction input or output
        Transaction transaction = context.getTransaction().get();
        TxRequest txRequest = ((TxRequest) event.getMessage().get());

        // Check if we are being given a signature
        TxRequestSerializedType serializedType = txRequest.getTxRequestSerializedType();
        if (serializedType.getSignatureIndex().isPresent()) {
          log.debug("Received signature index");
          int signedInputIndex = serializedType.getSignatureIndex().get();
          byte[] signature = serializedType.getSignature().get();
          context.getSignatures().put(signedInputIndex, signature);
        }
        if (serializedType.getSerializedTx().isPresent()) {
          log.debug("Received serialized Tx - could be partial");
          byte[] serializedTx = serializedType.getSerializedTx().get();
          try {
            context.getSerializedTx().write(serializedTx);
          } catch (IOException e) {
            // Ignore
          }
        }

        switch (txRequest.getTxRequestType()) {
          case TX_FINISHED:
            HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_SUCCEEDED, event.getMessage().get());
            break;
          case TX_OUTPUT:
            break;
        }
        client.txAck(txRequest, transaction, context.getReceivingAddressPathMap(), context.getChangeAddressPathMap());
        break;
      case BUTTON_REQUEST:
        // Device is requesting a button press
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_BUTTON_PRESS, event.getMessage().get());
        client.buttonAck();
        break;
      case FAILURE:
        // User has cancelled or operation failed
        HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_FAILED, event.getMessage().get());
        context.resetToInitialised();
        break;
      default:
        handleUnexpectedMessageEvent(context, event);
    }

  }
}
