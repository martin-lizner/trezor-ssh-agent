package org.multibit.hd.hardware.core.utils;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
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
   * @param tx The transaction to search
   * @param txHash The transaction hash to match on
   *
   * @return The matching transaction if present
   */
  public static Optional<Transaction> getTransactionByHash(Transaction tx, byte[] txHash) {

    // Assume the required transaction is the current (child) one
    Optional<Transaction> requiredTx = Optional.of(tx);

    if (!Arrays.areEqual(txHash, tx.getHash().getBytes())) {

      log.debug("Searching within tx inputs");

      // The child transaction does not match so look through all inputs
      for (TransactionInput txInput : tx.getInputs()) {
        Transaction parentTx = txInput.getParentTransaction();
        if (parentTx == null) {
          log.warn("Parent transaction for input {} is null", txInput);
        } else {
          if (!Arrays.areEqual(txHash, parentTx.getHash().getBytes())) {
            // Found matching parent
            log.debug("Located requested transaction by hash");
            requiredTx = Optional.of(txInput.getParentTransaction());
          }

        }
      }
    } else {
      log.debug("Requested transaction is current");
    }

    return requiredTx;
  }


}
