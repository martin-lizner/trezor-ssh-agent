package org.multibit.hd.hardware.examples.trezor;

import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <p>Utility to provide the following to tests:</p>
 * <ul>
 * <li>Various factory methods for creating addresses</li>
 * <li>Various factory methods for fake transactions</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class FakeTransactions {

  private static final Logger log = LoggerFactory.getLogger(FakeTransactions.class);

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

  private static ECKey asMainNetECKey(String dumpedPrivateKey, String expectedAddress) {

    try {

      // Read in the dumped private key
      DumpedPrivateKey privateKey = new DumpedPrivateKey(MainNetParams.get(), dumpedPrivateKey);
      ECKey key = privateKey.getKey();

      // Check the result
      Address actualAddress = new Address(MainNetParams.get(), key.getPubKeyHash());
      if (!actualAddress.toString().equals(expectedAddress)) {
        log.error("Actual address does not match expected address.\nE:'{}'\nA:'{}'", expectedAddress, actualAddress);
      } else {
        return key;
      }

    } catch (AddressFormatException e) {
      e.printStackTrace();
    }
    return null;
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
   *
   * @return A feeder, prev and current transaction
   */
  public static Transaction[] bip44DevWalletTransactions(Address destinationAddress, Address currentChangeAddress) {

    MainNetParams params = MainNetParams.get();

    // Deserialize a previous tx that spends to the Trezor receiving address by some other wallet e.g. MultiBit Classic
    // MultiBit Classic logs the actual transaction bytes sent so you can use that.
    // Send a transaction of size 1.3 mBTC
    Address currentChangeAddress;
    Address merchantAddress;
    try {
      merchantAddress = new Address(MainNetParams.get(), "189azcVcq5EDhXhRjAB9bt17g64KeXqidW");
      currentChangeAddress = new Address(MainNetParams.get(), "13pTZ2yZr6uY4Hw5mtvczLzvAbvhFkQAAc"); // 1/0
    } catch (AddressFormatException e) {
      log.error("Could not create address", e);
      return null;
    }

    // Deserialize a previous tx that spends to the Trezor receiving address

    /*
      Spent:
      1:
      01000000013ea3e61ae21fbfd8f6fdae08156a87bd7b985fe1e380e088d54aeb72fa0024ec010000006b483045022100dcaf9241a813699c584b664587d80219ea30ad0b847cec7c6b77aededb743f170220234d646304388ca13a9bebda84413b9e95218b73ceb3bd72a0f5a7d94ff29ef2012102f846445ee80fd95492ee3357257f588815ae8e077f6733e77b83d7d97dac3588ffffffff0240420f00000000001976a9149fb230929fcf2d4ed5fabd80cc33b5ef521bb89788acf91a5913000000001976a9143b0d3dc843fcce054271a7498d63f555548b16af88ac00000000
      Unspent:
      2:
      0100000001f6f11d2369f6034712ae4d2292b9dfaefb360494605c5d2c243a569ba94f2baf010000006b483045022100af01e1b3fafe0426eb67a9865b7373c82e3ff41e3a805f435fa904ad35563f70022050fbe55f7d43d3e2b8147ede76fae887a2744d71bf99bd4abae5d60dd4385339012103ced26a45356ae0b1b0993e641800719eaa078d21293e27571890ae8aa81180eaffffffff02d0fb0100000000001976a9149fb230929fcf2d4ed5fabd80cc33b5ef521bb89788ac484f0a00000000001976a91488bac377033ed520408d526420fc99c48b6fba7f88ac00000000
     */


    byte[] prevTxBytes = Utils.HEX.decode
      ("01000000017e38c4af61c39a64bb459f6c9976ba67a724c58098072ab194534f48b9f6cb0b010000006a47304402203d798df4ec3bcc310400eb46b90195b649a422121e461b08c17abcffb1679d8d0220371213153a8e2e30554af868a60d8a64dd705436d177d3e936843d05d76d33ba012103ced26a45356ae0b1b0993e641800719eaa078d21293e27571890ae8aa81180eaffffffff02d0fb0100000000001976a9145e5599d541ad44d341916c4a247dca214a2284b188acb6100600000000001976a91488bac377033ed520408d526420fc99c48b6fba7f88ac00000000");
      ("0100000001f6f11d2369f6034712ae4d2292b9dfaefb360494605c5d2c243a569ba94f2baf010000006b483045022100af01e1b3fafe0426eb67a9865b7373c82e3ff41e3a805f435fa904ad35563f70022050fbe55f7d43d3e2b8147ede76fae887a2744d71bf99bd4abae5d60dd4385339012103ced26a45356ae0b1b0993e641800719eaa078d21293e27571890ae8aa81180eaffffffff02d0fb0100000000001976a9149fb230929fcf2d4ed5fabd80cc33b5ef521bb89788ac484f0a00000000001976a91488bac377033ed520408d526420fc99c48b6fba7f88ac00000000");
    Transaction prevTx = new Transaction(params, prevTxBytes);
    TransactionOutput prevOut0 = prevTx.getOutput(0);

    // Create the current tx that spends to the donation address
    // Input 1.3mBTC               =  130_000sat
    // Outputs 1mBTC   (donation) =   100_000sat
    //         0.1mBTC (fee)      =    10_000sat
    //         0.2mBTC (change)   =    20_000sat
    // Create the current tx that spends to the merchant
    // Input 1.3mBTC               = 130_000sat
    // Outputs 1mBTC   (merchant) =  100_000sat
    //         0.1mBTC (fee)      =   10_000sat
    //         0.2mBTC (change)   =   20_000sat
    Transaction currentTx = new Transaction(params);
    TransactionOutput currentDonationOut = new TransactionOutput(params, currentTx, Coin.valueOf(100_000), destinationAddress);
    currentTx.addOutput(currentDonationOut);

    // Allow a small fee to facilitate the transaction going through
    TransactionOutput currentChangeOut = new TransactionOutput(params, currentTx, Coin.valueOf(20_000), currentChangeAddress);
    currentTx.addOutput(currentChangeOut);
    currentTx.addInput(prevOut0);

    // Log characteristics
    log.debug("Prev tx: {}", prevTx.getHashAsString());
    log.debug("Current tx: {}", currentTx.getHashAsString());

    return new Transaction[]{prevTx, currentTx};
  }

  /**
   * <p>Create a "MainNet" fake transaction consisting of:</p>
   * <ul>
   * <li>input[0]: random1 giving us bitcoins</li>
   * <li>output[0]: us giving random2 bitcoins</li>
   * <li>output[1]: us giving us change</li>
   * </ul>
   * <p>See {@link FakeTransactions#newMainNetAddress()} for new Addresses</p>
   *
   * @param ourReceivingAddress Our receiving address (present on the input from random1)
   * @param ourChangeAddress    Our change address
   * @param merchantAddress     An address that we are paying
   * @param inputSatoshis       The amount in satoshis we received (present on the input from from random1)
   * @param outputSatoshis      The amount in satoshis we are paying (will be used in each transaction)
   *
   * @return The transaction
   */
  public static Transaction newMainNetFakeTx(
    Address ourReceivingAddress,
    Address ourChangeAddress,
    Address merchantAddress,
    Coin inputSatoshis,
    Coin outputSatoshis) {

    Preconditions.checkState(inputSatoshis.compareTo(outputSatoshis) == 1, "Input must be greater than output");

    NetworkParameters params = MainNetParams.get();

    // Create the parent transaction
    Transaction parentTx = new Transaction(params);

    // Add an output to the parent
    TransactionInput parentInput = new TransactionInput(
      params,
      null,
      null
    );
    parentTx.addInput(parentInput);

    // Add an output to us
    TransactionOutput parentOutput = new TransactionOutput(
      params,
      parentTx,
      inputSatoshis,
      ourReceivingAddress
    );
    parentTx.addOutput(parentOutput);

    log.debug("Parent Tx hash: '{}' ({})", parentTx.getHashAsString(), parentTx.getHash().getBytes());

    // Create our transaction
    Transaction currentTx = new Transaction(params);

    // Add our parent's output as an input
    currentTx.addInput(parentOutput);

    // Create the output to merchant's address
    TransactionOutput merchantOutput = new TransactionOutput(
      params,
      currentTx,
      outputSatoshis,
      merchantAddress
    );
    currentTx.addOutput(merchantOutput);

    // Create the output to our change address
    TransactionOutput ourChangeOutput = new TransactionOutput(
      params,
      currentTx,
      inputSatoshis.subtract(outputSatoshis),
      ourChangeAddress
    );
    currentTx.addOutput(ourChangeOutput);

    log.debug("Current Tx hash: '{}' ({})", currentTx.getHashAsString(), currentTx.getHash().getBytes());

    return currentTx;
  }

}
