package org.multibit.hd.hardware.core.utils;

import org.bitcoinj.core.*;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.testing.FakeTxBuilder;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fest.assertions.api.Assertions.assertThat;


public class TransactionUtilsTest {

  private static final Logger log = LoggerFactory.getLogger(TransactionUtilsTest.class);

  private static final NetworkParameters PARAMS = UnitTestParams.get();
  private Transaction tx1;
  private Transaction tx2;
  private Transaction tx3;

  private Transaction dummy1;
  private Transaction dummy2;

  public static final Address ADDRESS1 = new ECKey().toAddress(PARAMS);
  public static final Address ADDRESS2 = new ECKey().toAddress(PARAMS);


  @Before
  public void setUp() throws Exception {

  }

  @Test
  /**
   * Check that a transaction is essentially equal to itself.
   * Two transactions are essentially equal if they spend the same inputs to the same outputs
   */
  public void testCheckEssentiallyEqual() throws Exception {
    // Check that a transaction is essentially equal to itself
    dummy1 = FakeTxBuilder.createFakeTx(PARAMS, Coin.COIN, ADDRESS1);
    tx1 = new Transaction(PARAMS);
    tx1.addInput(dummy1.getOutput(0));
    tx1.addOutput(Coin.COIN, ADDRESS1);

    // Different inputs, same outputs
    dummy2 = FakeTxBuilder.createFakeTx(PARAMS, Coin.COIN, ADDRESS2);
    tx2 = new Transaction(PARAMS);
    tx2.addInput(dummy2.getOutput(0));
    tx2.addOutput(Coin.COIN, ADDRESS1);

    // Different outputs, same inputs
    tx3 = new Transaction(PARAMS);
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
