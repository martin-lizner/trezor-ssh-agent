package org.multibit.hd.hardware.core.messages;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * <p>Value object to provide the following to high level messages:</p>
 * <ul>
 * <li>Transaction request details (index position, transaction hash etc)</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class TxRequestDetailsType {

  private final int index;
  private final Optional<byte[]> txHash;

  public TxRequestDetailsType(int requestIndex, byte[] txHash) {

    index = requestIndex;
    this.txHash = Optional.fromNullable(txHash);
  }

  public int getIndex() {
    return index;
  }

  public Optional<byte[]> getTxHash() {
    return txHash;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("index", index)
      .append("txHash", txHash)
      .toString();
  }
}