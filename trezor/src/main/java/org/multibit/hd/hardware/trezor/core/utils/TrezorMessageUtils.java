package org.multibit.hd.hardware.trezor.core.utils;

import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.params.MainNetParams;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.trezor.core.protobuf.MessageType;
import org.multibit.hd.hardware.trezor.core.protobuf.TrezorMessage;
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
    short headerCode = MessageType.getHeaderCode(message);

    // Provide some debugging
    MessageType messageType = MessageType.getMessageTypeByHeaderCode(headerCode);
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
    TrezorMessage.TxInput.Builder builder = TrezorMessage.TxInput.newBuilder();
    builder.setIndex(index);

    // Fill in the input addresses
    long prevIndex = txInput.getOutpoint().getIndex();
    byte[] prevHash = txInput.getOutpoint().getHash().getBytes();

    // In Bitcoinj "nanocoins" are Satoshis
    long satoshiAmount = txInput.getConnectedOutput().getValue().longValue();
    builder.setAmount(satoshiAmount);

    try {
      byte[] scriptSig = txInput.getScriptSig().toString().getBytes();
      builder.setScriptSig(ByteString.copyFrom(scriptSig));

      builder.setPrevIndex((int) prevIndex);
      builder.setPrevHash(ByteString.copyFrom(prevHash));

      builder.addAddressN(0);
      builder.addAddressN(index);

      return builder.build();

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
    TrezorMessage.TxOutput.Builder builder = TrezorMessage.TxOutput.newBuilder();
    builder.setIndex(index);

    // In Bitcoinj "nanocoins" are Satoshis
    long satoshiAmount = txOutput.getValue().longValue();
    builder.setAmount(satoshiAmount);

    // Extract the receiving address from the output
    try {
      builder.setAddress(txOutput.getScriptPubKey().getToAddress(MainNetParams.get()).toString());
    } catch (ScriptException e) {
      throw new IllegalArgumentException("Transaction script pub key invalid", e);
    }
    //builder.setAddressBytes(ByteString.copyFrom("".getBytes()));

    // Bitcoinj only support Pay to Address
    builder.setScriptType(TrezorMessage.ScriptType.PAYTOADDRESS);

    // TODO (GR) Verify what ScriptArgs is doing (array of script arguments?)
    //builder.setScriptArgs(0,0);

    // AddressN encodes the branch co-ordinates of the receiving/change public keys
    // Leave it unset if the Trezor does not control the output address
    if (index == 1) {
      builder.addAddressN(0); // Depth of receiving address was
      builder.addAddressN(1); // 0 is recipient address, 1 is change address
    }

    return builder.build();

  }
}
