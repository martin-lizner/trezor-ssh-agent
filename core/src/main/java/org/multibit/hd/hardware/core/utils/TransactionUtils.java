package org.multibit.hd.hardware.core.utils;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.Utils;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;

/**
 * <p>Utility class to provide the following to Bitcoinj transactions:</p>
 * <ul>
 * <li>Finding transactions by hash</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class TransactionUtils {

  private static final Logger log = LoggerFactory.getLogger(TransactionUtils.class);

  /**
   * Utilities have private constructors
   */
  private TransactionUtils() {
  }

  /**
   * @param tx     The transaction to search
   * @param txHash The transaction hash to match on
   *
   * @return The matching transaction if present
   */
  public static Optional<Transaction> getTransactionByHash(Transaction tx, byte[] txHash) {

    log.debug("Searching for {}", Utils.HEX.encode(txHash));

    if (Arrays.areEqual(txHash, tx.getHash().getBytes())) {
      log.debug("Requested transaction is current");
      return Optional.of(tx);
    }

    // The child transaction does not match so look through all inputs

    Optional<Transaction> requestedTx = Optional.absent();
    for (TransactionInput txInput : tx.getInputs()) {
      if (txInput.getOutpoint() == null) {
        log.warn("Outpoint for input {} is null", txInput);
        continue;
      }
      if (txInput.getOutpoint().getConnectedOutput() == null) {
        log.warn("Connected output for input {} is null", txInput);
        continue;
      }

      if (Arrays.areEqual(txHash, txInput.getOutpoint().getHash().getBytes())) {
        // Found matching parent
        log.debug("Located requested transaction.");
        Transaction parentTx = txInput.getOutpoint().getConnectedOutput().getParentTransaction();
        if (parentTx == null) {
          log.warn("Parent transaction for input {} is null", txInput);
          continue;
        }
        requestedTx = Optional.of(parentTx);
        break;
      }

      if (!requestedTx.isPresent()) {
        log.warn("Failed to locate requested transaction.");
      }

    }

    return requestedTx;

  }
}