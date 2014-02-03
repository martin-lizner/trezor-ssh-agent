package org.multibit.hd.hardware.trezor;

import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.params.MainNetParams;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.trezor.protobuf.TrezorMessage;
import org.multibit.hd.hardware.trezor.protobuf.TrezorProtocolMessageType;
import org.multibit.hd.hardware.trezor.protobuf.TrezorTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>Utility class to provide the following to applications:</p>
 * <ul>
 * <li>Various TrezorMessage related operations</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public final class TrezorMessageUtils {

  private static final Logger log = LoggerFactory.getLogger(TrezorMessageUtils.class);

  /**
   * Utilities should not have public constructors
   */
  private TrezorMessageUtils() {
  }

  /**
   * <p>Write a Trezor protocol buffer message to an OutputStream</p>
   *
   * @param message The protocol buffer message to read
   * @param out     The data output stream (must be open)
   *
   * @throws java.io.IOException If the device disconnects during IO
   */
  public static void writeMessage(Message message, DataOutputStream out) throws IOException {

    // Require the header code
    short headerCode = TrezorProtocolMessageType.getHeaderCode(message);

    // Provide some debugging
    TrezorProtocolMessageType messageType = TrezorProtocolMessageType.getMessageTypeByHeaderCode(headerCode);
    log.debug("> {}", messageType.name());

    // Write magic alignment string (avoiding immediate flush)
    out.writeBytes("##");

    // Write header following Python's ">HL" syntax
    // > = Big endian, std size and alignment
    // H = Unsigned short (2 bytes) for header code
    // L = Unsigned long (4 bytes) for message length

    // Message type
    out.writeShort(headerCode);

    // Message length
    out.writeInt(message.getSerializedSize());

    // Write the detail portion as a protocol buffer message
    message.writeTo(out);

    // Flush to ensure bytes are available immediately
    out.flush();

  }

  /**
   * <p>Construct a TxInput message based on the given transaction </p>
   *
   * @param tx    The Bitcoinj transaction
   * @param index The index of the input transaction to work with
   *
   * @return A TxInput message representing the transaction input
   */
  public static TrezorMessage.TxInput newTxInput(Transaction tx, int index) {

    Preconditions.checkNotNull(tx, "Transaction must be present");
    Preconditions.checkElementIndex(index, tx.getInputs().size(), "TransactionInput not present at index " + index);

    // Input index is valid
    TransactionInput txInput = tx.getInput(index);

    // Fill in the input addresses
    long prevIndex = txInput.getOutpoint().getIndex();
    byte[] prevHash = txInput.getOutpoint().getHash().getBytes();

    // TODO Locate amount field in TxInput
    long satoshiAmount = txInput.getConnectedOutput().getValue().longValue();

    try {
      byte[] scriptSig = txInput.getScriptSig().toString().getBytes();

      TrezorMessage.TxInput.Builder txInputBuilder = TrezorMessage.TxInput.newBuilder();

      txInputBuilder.setInput(TrezorTypes.TxInputType
        .newBuilder()
          // TODO (GR) integrate this
        .addAddressN(0)
        .addAddressN(index)
        .setScriptSig(ByteString.copyFrom(scriptSig))
        .setPrevHash(ByteString.copyFrom(prevHash))
        .setPrevIndex((int) prevIndex)
        .build()
      );

      return txInputBuilder.build();

    } catch (ScriptException e) {
      throw new IllegalStateException(e);
    }

  }

  /**
   * <p>Construct a TxOutput message based on the given transaction</p>
   *
   * @param tx    The Bitcoinj transaction
   * @param index The index of the output transaction to work with
   *
   * @return A TxOutput message representing the transaction output
   */
  public static TrezorMessage.TxOutput newTxOutput(Transaction tx, int index) {

    Preconditions.checkNotNull(tx, "Transaction must be present");
    Preconditions.checkElementIndex(index, tx.getOutputs().size(), "TransactionOutput not present at index " + index);

    // Output index is valid
    TransactionOutput txOutput = tx.getOutput(index);

    long satoshiAmount = txOutput.getValue().longValue();

    // Extract the Base58 receiving address from the output
    final byte[] address;
    try {
      address = txOutput.getScriptPubKey().getToAddress(MainNetParams.get()).toString().getBytes();
    } catch (ScriptException e) {
      throw new IllegalArgumentException("Transaction script pub key invalid", e);
    }

    TrezorMessage.TxOutput.Builder builder = TrezorMessage.TxOutput.newBuilder();
    builder.setOutput(TrezorTypes.TxOutputType
      .newBuilder()
      .setAddress(ByteString.copyFrom(address))
      .setAmount(satoshiAmount)
      .setScriptType(TrezorTypes.ScriptType.PAYTOADDRESS)
      .addAddressN(0) // Depth of receiving address
      .addAddressN(1) // 0 is recipient address, 1 is change address

        // TODO (GR) Verify what ScriptArgs is doing (array of script arguments?)
        //.setScriptArgs(0,0);
      .build()
    );

    return builder.build();

  }
}
