package org.multibit.hd.hardware.core.messages;

import com.google.bitcoin.core.*;
import com.google.bitcoin.params.MainNetParams;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * <p>Utility to provide the following to tests:</p>
 * <ul>
 * <li>Various factory methods for creating addresses</li>
 * <li>Various factory methods for fake transactions</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class FakeTransactions {

  private static Map<Integer, Address> receivingAddresses = Maps.newHashMap();
  private static Map<Integer, Address> changeAddresses = Maps.newHashMap();

  static {

    receivingAddresses.put(0, asMainNetAddress("1KqYyzL53R8oA1LdYvyv7m6JUryFfGJDpa"));
    receivingAddresses.put(2, asMainNetAddress("13MzKU6YjjdyiW3dZJDa5VU4AWGczQsdYD"));
    receivingAddresses.put(3, asMainNetAddress("1FQVPnjrbkPWeA8poUoEnX9U3n9DyhAVtv"));
    receivingAddresses.put(9, asMainNetAddress("1C9DHmWBpvGcFKXEiWWC3EK3EY5Bj79nze"));

    changeAddresses.put(0, asMainNetAddress("17GpAFnkHRjWKePkX4kxHaHy49V8EHTr7i"));
    changeAddresses.put(2, asMainNetAddress("1MVgq4XaMX7PmohkYzFEisH1D7uxTiPbFK"));
    changeAddresses.put(3, asMainNetAddress("1M5NSqrUmmkZqokpHsJd5xm74YG6kjVcz4"));
    changeAddresses.put(9, asMainNetAddress("1BXUkUsc5gGSzYUAEebg5WZWtRGPNW4NQ9"));
  }

  /**
   * Utilities do not have public constructors
   */
  private FakeTransactions() {


  }

  /**
   * <p>Provide working addresses for the Electrum HD algorithm based on a seed of:</p>
   * <code>beyond neighbor scratch swirl embarrass doll cause also stick softly physical nice</code>
   * <p>The Electrum algorithm uses 2 branches: one for receiving addresses, the other for change addresses</p>
   * <ul>
   * <li>branch[0] = index within branch</li>
   * <li>branch[1] = 0 or 1 where 0 is receiving address, 1 is a change address</li>
   * </ul>
   *
   * @param branch The branch information for the Electrum hierarchical deterministic algorithm
   *
   * @return The main net address
   */
  public static Address getElectrumAddressN(int[] branch) {

    Preconditions.checkState(branch.length == 2, "Branch information must be 2 elements");

    // Determine which type of address we want (receiving or change)
    if (branch[1] == 0) {
      return receivingAddresses.get(branch[0]);
    } else {
      return changeAddresses.get(branch[0]);
    }

  }

  /**
   * @param address The main net address (e.g. "1KzTSfqjF2iKCduwz59nv2uqh1W2JsTxZH")
   *
   * @return A new main net address
   */
  public static Address asMainNetAddress(String address) {

    try {
      return new Address(MainNetParams.get(), address);
    } catch (AddressFormatException e) {
      throw new IllegalArgumentException(e);
    }

  }

  /**
   * @return A new main net address
   */
  public static Address newMainNetAddress() {

    return new ECKey().toAddress(MainNetParams.get());

  }

  /**
   * <p>Create a "MainNet" fake transaction consisting of:</p>
   * <ul>
   * <li>input[0]: random1 giving us bitcoins</li>
   * <li>output[0]: us giving random2 bitcoins</li>
   * <li>output[1]: us giving us change</li>
   * </ul>
   * <p>See {@link FakeTransactions#newMainNetAddress()} for new Addresses</p>
   * <p>Note that in Bitcoinj a satoshi is a "nanocoin"</p>
   *
   * @param ourReceivingAddress Our receiving address (present on the input from random1)
   * @param ourChangeAddress    Our change address
   * @param random2Address      An address that we are paying
   * @param inputSatoshis       The amount in satoshis we received (present on the input from from random1)
   * @param outputSatoshis      The amount in satoshis we are paying (will be used in each transaction)
   *
   * @return The transaction
   */
  public static Transaction newMainNetFakeTx(
    Address ourReceivingAddress,
    Address ourChangeAddress,
    Address random2Address,
    Coin inputSatoshis,
    Coin outputSatoshis) {

    Preconditions.checkState(inputSatoshis.compareTo(outputSatoshis) == 1, "Input must be greater than output");

    NetworkParameters params = MainNetParams.get();

    // Create the transaction from random1 (our parent)
    Transaction random1Tx = new Transaction(params);

    // Add an output to us
    TransactionOutput random1Output = new TransactionOutput(
      params,
      random1Tx,
      inputSatoshis,
      ourReceivingAddress
    );
    random1Tx.addOutput(random1Output);

    // Create our transaction
    Transaction ourTx = new Transaction(params);

    // Add our parent as an input
    ourTx.addInput(random1Output);

    // Create the output to random2
    TransactionOutput random2Output = new TransactionOutput(
      params,
      ourTx,
      outputSatoshis,
      random2Address
    );
    ourTx.addOutput(random2Output);

    // Create the output to our change
    TransactionOutput ourChangeOutput = new TransactionOutput(
      params,
      ourTx,
      inputSatoshis.subtract(outputSatoshis),
      ourChangeAddress
    );
    ourTx.addOutput(ourChangeOutput);

    return ourTx;
  }

}
