package org.multibit.hd.hardware.examples.trezor;

import com.google.bitcoin.core.*;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
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

  public static final String MERCHANT_PRIVATE_KEY = "Kwbs5ibVcoLTNLDQey382v7PAs5gQPWTrqi3qMxNHReSSLr3BSFf";
  public static final String MERCHANT_ADDRESS = "189azcVcq5EDhXhRjAB9bt17g64KeXqidW";

  public static final String PREV_PRIVATE_KEY = "L3Jcdi3MuYwu5Vwcude5FBPAQPBT7LUhwA5rZFo4d7BqWsWmi8bD";
  public static final String PREV_ADDRESS = "1EQ8kwmubETKbQxEVfMbLffYRenyuvFXGW";

  public static final ECKey merchantKey = asMainNetECKey(MERCHANT_PRIVATE_KEY, MERCHANT_ADDRESS);
  public static final ECKey prevKey = asMainNetECKey(PREV_PRIVATE_KEY, PREV_ADDRESS);




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
   * <ul>
   * <li>input[0]: random1 giving us bitcoins</li>
   * <li>output[0]: us giving random2 bitcoins</li>
   * <li>output[1]: us giving us change</li>
   * </ul>
   * <p>See {@link FakeTransactions#newMainNetAddress()} for new Addresses</p>
   *
   * @param currentReceivingAddress Our receiving address (present on the input from random1)
   * @param currentChangeAddress    Our change address
   * @param inputSatoshis           The amount in satoshis we received (present on the input from from random1)
   * @param outputSatoshis          The amount in satoshis we are paying (will be used in each transaction)
   *
   * @return The transaction
   */
  public static Transaction newMainNetFakeTx2(
    Address currentReceivingAddress,
    Address currentChangeAddress,
    Coin inputSatoshis,
    Coin outputSatoshis) {

    MainNetParams params = MainNetParams.get();

    Address prevAddress = new Address(MainNetParams.get(), prevKey.getPubKeyHash());
    Address merchantAddress = new Address(MainNetParams.get(), merchantKey.getPubKeyHash());

    // Create the feeder tx providing the previous tx with funds
    Transaction feederTx = new Transaction(params);
    TransactionOutput feederOut = new TransactionOutput(params, feederTx, inputSatoshis, prevAddress);
    feederTx.addOutput(feederOut);

    // Create the previous tx that sends from the feeder to the current
    Transaction prevTx = new Transaction(params);
    TransactionOutput prevOut = new TransactionOutput(params, prevTx, inputSatoshis, currentReceivingAddress);
    prevTx.addOutput(prevOut);

    TransactionInput prevTxIn = prevTx.addInput(feederOut);

    // Sign prevTx
    Script scriptPubKey = prevTxIn.getConnectedOutput().getScriptPubKey();
    prevTxIn.setScriptSig(scriptPubKey.createEmptyInputScript(prevKey, prevOut.getScriptPubKey()));

    Script inputScript = prevTxIn.getScriptSig();

    byte[] script =prevOut.getScriptPubKey().getProgram();
    try {
      TransactionSignature signature = prevTx.calculateSignature(0, prevKey, script, Transaction.SigHash.ALL, false);

      // at this point we have incomplete inputScript with OP_0 in place of one or more signatures. We already
      // have calculated the signature using the local key and now need to insert it in the correct place
      // within inputScript. For pay-to-address and pay-to-key script there is only one signature and it always
      // goes first in an inputScript (sigIndex = 0). In P2SH input scripts we need to figure out our relative
      // position relative to other signers.  Since we don't have that information at this point, and since
      // we always run first, we have to depend on the other signers rearranging the signatures as needed.
      // Therefore, always place as first signature.
      int sigIndex = 0;
      inputScript = scriptPubKey.getScriptSigWithSignature(inputScript, signature.encodeToBitcoin(), sigIndex);
      prevTxIn.setScriptSig(inputScript);

    } catch (ECKey.KeyIsEncryptedException e) {
      throw e;
    } catch (ECKey.MissingPrivateKeyException e) {
      log.warn("No private key in keypair for input {}", 0);
    }

    // Create the current tx that sends from the previous to the merchant
    Transaction currentTx = new Transaction(params);
    TransactionOutput currentMerchantOut = new TransactionOutput(params, currentTx, outputSatoshis, merchantAddress);
    currentTx.addOutput(currentMerchantOut);

    TransactionOutput currentChangeOut = new TransactionOutput(params, currentTx, inputSatoshis.subtract(outputSatoshis), currentChangeAddress);
    currentTx.addOutput(currentChangeOut);

    currentTx.addInput(prevOut);

    // Roundtrip the tx so that they are just like they would be from the wire
//    Transaction[] txs= new Transaction[]{
//      FakeTxBuilder.roundTripTransaction(params, prevTx),
//      FakeTxBuilder.roundTripTransaction(params, currentTx)
//    };

    // Log characteristics
    log.debug("Feeder tx: {}", feederTx.getHashAsString());
    log.debug("Prev tx: {}", prevTx.getHashAsString());
    log.debug("Current tx: {}", currentTx.getHashAsString());

    // Return the roundtripped current tx
    return currentTx;
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
