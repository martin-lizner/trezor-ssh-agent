package org.multibit.hd.hardware.core.messages;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * <p>Value object to provide the following to high level messages:</p>
 * <ul>
 * <li>Transaction request details (requestIndex, txHash)</li>
 * </ul>
 * <p>This provides meta information regarding how transactions should be located and inputs/outputs explored</p>
 *
 * @since 0.0.1
 *  
 */
public class TxRequestDetailsType {

  private final Optional<Integer> requestIndex;
  private final Optional<byte[]> txHash;

  public TxRequestDetailsType(
    boolean hasRequestIndex,
    int requestIndex,
    boolean hasTxHash,
    byte[] txHash
  ) {

    // Drive presence/absence from the flags rather than content
    if (hasRequestIndex) {
      this.requestIndex = Optional.of(requestIndex);
    } else {
      this.requestIndex = Optional.absent();
    }
    if (hasTxHash) {
      this.txHash = Optional.of(txHash);
    } else {
      this.txHash = Optional.absent();
    }
  }

  public Optional<Integer> getRequestIndex() {
    return requestIndex;
  }

  public Optional<byte[]> getTxHash() {
    return txHash;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("requestIndex", requestIndex)
      .append("txHash", txHash)
      .toString();
  }
}