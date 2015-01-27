package org.multibit.hd.hardware.core.utils;

import org.bitcoinj.core.*;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.testing.FakeTxBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;


public class TransactionUtilsTest {

  private static final NetworkParameters PARAMS = UnitTestParams.get();

  private static final Address ADDRESS1 = new ECKey().toAddress(PARAMS);
  private static final Address ADDRESS2 = new ECKey().toAddress(PARAMS);

  @Before
  public void setUp() throws Exception {

  }

  /**
   * Check that a transaction is essentially equal to itself.
   * Two transactions are essentially equal if they spend the same inputs to the same outputs
   */
  @Test
  public void testCheckEssentiallyEqual() throws Exception {

    // Check that a transaction is essentially equal to itself
    Transaction dummy1 = FakeTxBuilder.createFakeTx(PARAMS, Coin.COIN, ADDRESS1);
    Transaction tx1 = new Transaction(PARAMS);
    tx1.addInput(dummy1.getOutput(0));
    tx1.addOutput(Coin.COIN, ADDRESS1);

    // Different inputs, same outputs
    Transaction dummy2 = FakeTxBuilder.createFakeTx(PARAMS, Coin.COIN, ADDRESS2);
    Transaction tx2 = new Transaction(PARAMS);
    tx2.addInput(dummy2.getOutput(0));
    tx2.addOutput(Coin.COIN, ADDRESS1);

    // Different outputs, same inputs
    Transaction tx3 = new Transaction(PARAMS);
    tx3.addInput(dummy1.getOutput(0));
    tx3.addOutput(Coin.COIN, ADDRESS2);

    assertThat(TransactionUtils.checkEssentiallyEqual(tx1, tx1)).isTrue();

    // Different inputs, same outputs
    assertThat(TransactionUtils.checkEssentiallyEqual(tx1, tx2)).isFalse();
    assertThat(TransactionUtils.checkEssentiallyEqual(tx2, tx1)).isFalse();

    // Different outputs, same inputs
    assertThat(TransactionUtils.checkEssentiallyEqual(tx1, tx3)).isFalse();
    assertThat(TransactionUtils.checkEssentiallyEqual(tx3, tx1)).isFalse();
  }
}
