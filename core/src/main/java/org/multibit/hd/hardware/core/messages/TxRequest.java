package org.multibit.hd.hardware.core.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Description of why the device requires transaction details</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class TxRequest implements HardwareWalletMessage {

  private final TxRequestType txRequestType;
  private final TxRequestDetailsType txRequestDetailsType;
  private final TxRequestSerializedType txRequestSerializedType;

  public TxRequest(TxRequestType txRequestType, TxRequestDetailsType txRequestDetailsType, TxRequestSerializedType txRequestSerializedType) {

    this.txRequestType = txRequestType;
    this.txRequestDetailsType = txRequestDetailsType;
    this.txRequestSerializedType = txRequestSerializedType;
  }

  public TxRequestType getTxRequestType() {
    return txRequestType;
  }

  public TxRequestDetailsType getTxRequestDetailsType() {
    return txRequestDetailsType;
  }

  public TxRequestSerializedType getTxRequestSerializedType() {
    return txRequestSerializedType;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("txRequestType", txRequestType)
      .append("txRequestDetailsType", txRequestDetailsType)
      .append("txRequestSerializedType", txRequestSerializedType)
      .toString();
  }
}
