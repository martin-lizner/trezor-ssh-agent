package org.multibit.hd.hardware.examples.keepkey.wallet;

import org.bitcoinj.signers.TransactionSigner;
import org.bitcoinj.wallet.KeyBag;

/**
 * <p>Transaction signer to provide the following to Bitcoinj transactions:</p>
 * <ul>
 * <li>Ability to sign transactions using Trezor device</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class KeepKeyTransactionSigner implements TransactionSigner {

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public byte[] serialize() {
    return new byte[0];
  }

  @Override
  public void deserialize(byte[] data) {

  }

  @Override
  public boolean signInputs(ProposedTransaction propTx, KeyBag keyBag) {
    return false;
  }
}
