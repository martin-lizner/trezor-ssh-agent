package org.multibit.hd.hardware.core.utils;

import com.google.common.base.Optional;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;

import java.util.List;

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

  /**
   * Check that the signedTransaction returned from the hardware wallet is essentially the same as the unsignedTransaction
   * that was sent to it
   *
   * @param unsignedTransaction the unsignedTransaction sent to the hardware signing device
   * @param signedTransaction   the signed transaction returned from te hardware signing device
   * @return true if the transactions are essentially the same, false otherwise
   */
  public static boolean checkEssentiallyEqual(Transaction unsignedTransaction, Transaction signedTransaction) {
    // Check there are the same number of transactionInputs and transactionOutputs
    List<TransactionInput> unsignedTransactionInputs = unsignedTransaction.getInputs();
    List<TransactionOutput> unsignedTransactionOutputs = unsignedTransaction.getOutputs();

    List<TransactionInput> signedTransactionInputs = signedTransaction.getInputs();
    List<TransactionOutput> signedTransactionOutputs = signedTransaction.getOutputs();

    if (unsignedTransactionInputs.size() != signedTransactionInputs.size() ||
            unsignedTransactionOutputs.size() != signedTransactionOutputs.size()) {
      return false;
    }

    // Check every transactionInput on the unsigned tx appears on the signed tx
    for (TransactionInput unsignedTxInput : unsignedTransactionInputs) {
      if (!checkTransactionInputs(unsignedTxInput, signedTransactionInputs)) {
        return false;
      }
    }

    // Check every transactionOutput on the unsigned tx appears on the signed tx
    for (TransactionOutput unsignedTxOutput : unsignedTransactionOutputs) {
      if (!checkTransactionOutputs(unsignedTxOutput, signedTransactionOutputs)) {
        return false;
      }
    }

   // Check every transactionInput on the signed tx appears on the unsigned tx
    for (TransactionInput signedTxInput : signedTransactionInputs) {
      if (!checkTransactionInputs(signedTxInput, unsignedTransactionInputs)) {
        return false;
      }
    }

    // Check every transactionOutput on the signed tx appears on the unsigned tx
    for (TransactionOutput signedTxOutput : signedTransactionOutputs) {
      if (!checkTransactionOutputs(signedTxOutput, unsignedTransactionOutputs)) {
        return false;
      }
    }

    return true;
  }

  private static boolean checkTransactionInputs(TransactionInput txInputToCheck, List<TransactionInput> txInputs) {
    // Find the matching transactionInput, matching by the outpoint the transaction input it is spending
    for (TransactionInput txInput : txInputs) {
      if (txInputToCheck.getOutpoint() != null) {
        if (txInputToCheck.getOutpoint().equals(txInput.getOutpoint())) {
          return true;
        }
      } else {
        // Output is null, matching fails
        log.debug("No output to match on, matching fails");
      }
    }
    return false;
  }


  private static boolean checkTransactionOutputs(TransactionOutput txOutputToCheck, List<TransactionOutput> txOutputs) {
    // Find the matching transactionOutput, matching by the output script bytes

    for (TransactionOutput txOutput : txOutputs) {
      if (txOutputToCheck.getScriptBytes() != null) {
        if (Arrays.areEqual(txOutputToCheck.getScriptBytes(), txOutput.getScriptBytes())) {
          return true;
        }
      } else {
        // Script bytes are null
        log.debug("No script bytes to match on, matching fails");
      }
    }
    return false;
  }
}