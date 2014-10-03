package org.multibit.hd.hardware.trezor.utils;

import com.google.bitcoin.core.Utils;
import com.google.protobuf.ByteString;
import com.satoshilabs.trezor.protobuf.TrezorType;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * <p>Apache Commons ToStringStyle extension to provide the following to logging:</p>
 * <ul>
 * <li>Hex representation of byte[], ByteString</li>
 * <li>Better representation of custom Trezor message types</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class TrezorMessageToStringStyle extends ToStringStyle {

  /**
   * Match the multi-line style
   */
  public TrezorMessageToStringStyle() {
    super();
    this.setContentStart("[");
    this.setFieldSeparator(SystemUtils.LINE_SEPARATOR + "  ");
    this.setFieldSeparatorAtStart(true);
    this.setContentEnd(SystemUtils.LINE_SEPARATOR + "]");
  }

  protected void appendDetail(StringBuffer buffer, String fieldName, Object value) {

    if (value instanceof byte[]) {

      byte[] bytes = (byte[]) value;
      appendHexBytes(buffer, bytes);

    } else if (value instanceof ByteString) {

      byte[] bytes = ((ByteString) value).toByteArray();
      appendHexBytes(buffer, bytes);

    } else if (value instanceof TrezorType.TxRequestDetailsType) {

      TrezorType.TxRequestDetailsType detail = ((TrezorType.TxRequestDetailsType) value);
      appendTxRequestDetailsType(buffer, detail);

    } else if (value instanceof TrezorType.TransactionType) {

      TrezorType.TransactionType txType = ((TrezorType.TransactionType) value);
      appendTransactionType(buffer, txType);

    } else {

      buffer.append(value);
    }

  }

  private void appendHexBytes(StringBuffer buffer, byte[] bytes) {
    for (byte b : bytes) {
      buffer.append(String.format(" %02x", b));
    }
  }

  private void appendTransactionType(StringBuffer buffer, TrezorType.TransactionType txType) {

    for (TrezorType.TxInputType txInputType : txType.getInputsList()) {
      appendTxInputType(buffer, txInputType);
    }
//    for (TrezorType.TxOutputType txOutputType : txAck.getTx().getOutputsList()) {
//      appendTxOutputType(buffer, txOutputType);
//    }

  }

  private void appendTxRequestDetailsType(StringBuffer buffer, TrezorType.TxRequestDetailsType detail) {
    buffer
      .append("\n    request_index: ")
      .append(detail.getRequestIndex())
      .append("\n    tx_hash: ");

    byte[] bytes = detail.getTxHash().toByteArray();
    if (bytes.length>0) {
      buffer.append(Utils.HEX.encode(bytes));
    }
  }

  private void appendTxInputType(StringBuffer buffer, TrezorType.TxInputType txInput) {
    for (Integer addressN: txInput.getAddressNList()) {
      buffer
        .append("\n    address_n: ")
        .append(addressN);
    }
    byte[] bytes = txInput.getPrevHash().toByteArray();
    if (bytes.length>0) {
      buffer
        .append("\n    prev_hash: ")
        .append(Utils.HEX.encode(bytes));
    }

    buffer
      .append("\n    prev_index: ")
      .append(txInput.getPrevIndex());

    buffer
      .append("\n    script_type: ")
      .append(txInput.getScriptType());

    bytes = txInput.getScriptSig().toByteArray();
    if (bytes.length>0) {
      buffer
        .append("\n    script_sig: ")
        .append(Utils.HEX.encode(bytes));
    }
  }
}