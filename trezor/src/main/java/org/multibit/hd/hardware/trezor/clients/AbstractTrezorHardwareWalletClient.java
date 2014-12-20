package org.multibit.hd.hardware.trezor.clients;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.KeyChain;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.messages.TxRequest;
import org.multibit.hd.hardware.core.utils.TransactionUtils;
import org.multibit.hd.hardware.trezor.utils.TrezorMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>Hardware wallet client to provide the following to Trezor clients:</p>
 * <ul>
 * <li>Access to common methods</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public abstract class AbstractTrezorHardwareWalletClient implements HardwareWalletClient {

  private static final Logger log = LoggerFactory.getLogger(AbstractTrezorHardwareWalletClient.class);

  @Override
  public Optional<MessageEvent> initialise() {
    return sendMessage(
      TrezorMessage.Initialize
        .newBuilder()
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> ping() {
    return sendMessage(
      TrezorMessage.Ping
        .newBuilder()
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> clearSession() {
    return sendMessage(
      TrezorMessage.ClearSession
        .newBuilder()
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> changePIN(boolean remove) {

    return sendMessage(
      TrezorMessage.ChangePin
        .newBuilder()
        .setRemove(remove)
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> wipeDevice() {
    return sendMessage(
      TrezorMessage.WipeDevice
        .newBuilder()
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> firmwareErase() {
    throw new UnsupportedOperationException("Use the mytrezor.com website for firmware upgrades.");
  }

  @Override
  public Optional<MessageEvent> firmwareUpload() {
    throw new UnsupportedOperationException("Use the mytrezor.com website for firmware upgrades.");
  }

  @Override
  public Optional<MessageEvent> getEntropy() {
    return sendMessage(
      TrezorMessage.GetEntropy
        .newBuilder()
        .build()
    );
  }

  // TODO Support use of seed node
  @Override
  public Optional<MessageEvent> loadDevice(
    String language,
    String label,
    String seedPhrase,
    String pin
  ) {

    // Define the node
    TrezorType.HDNodeType nodeType = TrezorType.HDNodeType
      .newBuilder()
      .setChainCode(ByteString.copyFromUtf8(""))
      .setChildNum(0)
      .setDepth(0)
      .setFingerprint(0)
      .build();

    // A load normally takes about 10 seconds to complete
    return sendMessage(
      TrezorMessage.LoadDevice
        .newBuilder()
        .setLanguage(language)
        .setLabel(label)
        .setMnemonic(seedPhrase)
//        .setNode(nodeType)
        .setPin(pin)
        .build()
    );

  }

  @Override
  public Optional<MessageEvent> resetDevice(
    String language,
    String label,
    boolean displayRandom,
    boolean pinProtection,
    int strength
  ) {

    return sendMessage(
      TrezorMessage.ResetDevice
        .newBuilder()
        .setLanguage(language)
        .setLabel(label)
        .setDisplayRandom(displayRandom)
        .setStrength(strength)
        .setPinProtection(pinProtection)
        .build()
    );

  }

  @Override
  public Optional<MessageEvent> recoverDevice(
    String language,
    String label,
    int wordCount,
    boolean passphraseProtection,
    boolean pinProtection
  ) {
    return sendMessage(
      TrezorMessage.RecoveryDevice
        .newBuilder()
        .setLanguage(language)
        .setLabel(label)
        .setWordCount(wordCount)
        .setPassphraseProtection(passphraseProtection)
        .setPinProtection(pinProtection)
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> wordAck(String word) {
    return sendMessage(
      TrezorMessage.WordAck
        .newBuilder()
        .setWord(word)
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> signTx(Transaction tx) {

    return sendMessage(
      TrezorMessage.SignTx
        .newBuilder()
        .setCoinName("Bitcoin")
        .setInputsCount(tx.getInputs().size())
        .setOutputsCount(tx.getOutputs().size())
        .build()
    );

  }

  @Override
  public Optional<MessageEvent> simpleSignTx(Transaction tx) {

    TrezorMessage.SimpleSignTx.Builder builder = TrezorMessage.SimpleSignTx.newBuilder();
    builder.setCoinName("Bitcoin");

    // Explore the current tx inputs
    for (TransactionInput input : tx.getInputs()) {

      // Build a TxInputType message

      int prevIndex = (int) input.getOutpoint().getIndex();
      byte[] prevHash = input.getOutpoint().getHash().getBytes();

      // No multisig support in MBHD yet
      TrezorType.InputScriptType inputScriptType = TrezorType.InputScriptType.SPENDADDRESS;

      TrezorType.TxInputType txInputType = TrezorType.TxInputType
        .newBuilder()
        .setSequence((int) input.getSequenceNumber())
        .setScriptSig(ByteString.copyFrom(input.getScriptSig().getProgram()))
        .setScriptType(inputScriptType)
        .setPrevIndex(prevIndex)
        .setPrevHash(ByteString.copyFrom(prevHash))
        .build();

      builder.addInputs(txInputType);
    }

    // Explore the current tx outputs
    for (TransactionOutput output : tx.getOutputs()) {

      // Build a TxOutputType message

      // Address
      Address address = output.getAddressFromP2PKHScript(MainNetParams.get());
      if (address == null) {
        throw new IllegalArgumentException("'address' must be present");
      }
      // Is it pay-to-script-hash (P2SH) or pay-to-address (P2PKH)?
      final TrezorType.OutputScriptType outputScriptType;
      if (address.isP2SHAddress()) {
        outputScriptType = TrezorType.OutputScriptType.PAYTOSCRIPTHASH;
      } else {
        outputScriptType = TrezorType.OutputScriptType.PAYTOADDRESS;
      }

      TrezorType.TxOutputType txOutputType = TrezorType.TxOutputType
        .newBuilder()
        .setAddress(String.valueOf(address))
        .setAmount(output.getValue().value)
        .setScriptType(outputScriptType)
        .build();

      builder.addOutputs(txOutputType);
    }

    // Explore the current tx inputs
    for (TransactionInput input : tx.getInputs()) {

      // Get the previous Tx
      Transaction prevTx = input.getOutpoint().getConnectedOutput().getParentTransaction();

      // No multisig support in MBHD yet
      TrezorType.TransactionType.Builder prevBuilder = TrezorType.TransactionType.newBuilder();

      // Explore the current tx inputs
      for (TransactionInput prevInput : prevTx.getInputs()) {

        // Build a TxInputType message

        int prevIndex = (int) prevInput.getOutpoint().getIndex();
        byte[] prevHash = prevInput.getOutpoint().getHash().getBytes();

        // No multisig support in MBHD yet
        TrezorType.InputScriptType inputScriptType = TrezorType.InputScriptType.SPENDADDRESS;

        TrezorType.TxInputType txInputType = TrezorType.TxInputType
          .newBuilder()
          .setSequence((int) prevInput.getSequenceNumber())
          .setScriptSig(ByteString.copyFrom(prevInput.getScriptSig().getProgram()))
          .setScriptType(inputScriptType)
          .setPrevIndex(prevIndex)
          .setPrevHash(ByteString.copyFrom(prevHash))
          .build();

        prevBuilder.addInputs(txInputType);
      }

      // Explore the current tx outputs
      for (TransactionOutput prevOutput : prevTx.getOutputs()) {

        // Build a TxOutputType message

        TrezorType.TxOutputBinType txOutputBinType = TrezorType.TxOutputBinType
          .newBuilder()
          .setAmount(prevOutput.getValue().value)
          .setScriptPubkey(ByteString.copyFrom(prevOutput.getScriptPubKey().getProgram()))
          .build();

        prevBuilder.addBinOutputs(txOutputBinType);
      }


      builder.addTransactions(prevBuilder.build());
    }


    // Send the message
    return sendMessage(builder.build());
  }

  @Override
  public Optional<MessageEvent> txAck(
    TxRequest txRequest,
    Transaction tx,
    Map<Integer, ImmutableList<ChildNumber>> receivingAddressPathMap,
    Map<Address, ImmutableList<ChildNumber>> changeAddressPathMap) {

    TrezorType.TransactionType txType = null;

    // Get the transaction hash (if present)
    Optional<byte[]> txHash = txRequest.getTxRequestDetailsType().getTxHash();

    // Assume we're working with the current (child) transaction to start with
    Optional<Transaction> requestedTx = Optional.of(tx);

    // Check if the requested transaction is different to the current
    boolean binOutputType = txHash.isPresent();
    if (binOutputType) {
      // Need to look up a transaction by hash
      requestedTx = TransactionUtils.getTransactionByHash(tx, txHash.get());

      // Check if the transaction was found
      if (!requestedTx.isPresent()) {
        log.error("Device requested unknown hash: {}", Utils.HEX.encode(txHash.get()));
        throw new IllegalArgumentException("Device requested unknown hash.");
      }

    }

    // Have the required transaction at this point

    switch (txRequest.getTxRequestType()) {
      case TX_META:
        txType = TrezorMessageUtils.buildTxMetaResponse(requestedTx);
        break;
      case TX_INPUT:
        txType = TrezorMessageUtils.buildTxInputResponse(txRequest, requestedTx, binOutputType, receivingAddressPathMap);
        break;
      case TX_OUTPUT:
        txType = TrezorMessageUtils.buildTxOutputResponse(txRequest, requestedTx, binOutputType, changeAddressPathMap);
        break;
      case TX_FINISHED:
        log.info("TxSign workflow complete.");
        break;
      default:
        log.error("Unknown TxReturnType: {}", txRequest.getTxRequestType().name());
        return Optional.absent();
    }

    // Must have fully processed the message to be here

    if (txType != null) {
      return sendMessage(
        TrezorMessage.TxAck
          .newBuilder()
          .setTx(txType)
          .build()
      );
    }

    log.info("No TxAck required.");

    return Optional.absent();

  }

  @Override
  public Optional<MessageEvent> pinMatrixAck(String pin) {
    return sendMessage(
      TrezorMessage.PinMatrixAck
        .newBuilder()
        .setPin(pin)
        .build(),
      // No immediate response expected
      2, TimeUnit.SECONDS
    );
  }

  @Override
  public Optional<MessageEvent> buttonAck() {
    return sendMessage(
      TrezorMessage.ButtonAck
        .newBuilder()
        .build(),
      // No immediate response expected
      2, TimeUnit.SECONDS
    );
  }

  public Optional<MessageEvent> cancel() {
    return sendMessage(
      TrezorMessage.Cancel
        .newBuilder()
        .build(),
      // No immediate response expected
      2, TimeUnit.SECONDS);
  }

  @Override
  public Optional<MessageEvent> getAddress(
    int account,
    KeyChain.KeyPurpose keyPurpose,
    int index,
    boolean showDisplay
  ) {

    return sendMessage(
      TrezorMessage.GetAddress
        .newBuilder()
          // Build the chain code
        .addAllAddressN(TrezorMessageUtils.buildAddressN(account, keyPurpose, index))
        .setCoinName("Bitcoin")
        .setShowDisplay(showDisplay)
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> getPublicKey(
    int account,
    KeyChain.KeyPurpose keyPurpose,
    int index
  ) {

    return sendMessage(
      TrezorMessage.GetPublicKey
        .newBuilder()
          // Build the chain code
        .addAllAddressN(TrezorMessageUtils.buildAddressN(account, keyPurpose, index))
        .build()
    );

  }

  @Override
  public Optional<MessageEvent> getDeterministicHierarchy(List<ChildNumber> childNumbers) {

    List<Integer> addressN = Lists.newArrayList();
    for (ChildNumber childNumber : childNumbers) {
      addressN.add(childNumber.getI());
    }

    return sendMessage(
      TrezorMessage.GetPublicKey
        .newBuilder()
        .addAllAddressN(addressN)
        .build()
    );

  }

  @Override
  public Optional<MessageEvent> applySettings(String language, String label) {

    return sendMessage(
      TrezorMessage.ApplySettings
        .newBuilder()
        .setLanguage(language)
        .setLabel(label)
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> entropyAck(byte[] entropy) {
    return sendMessage(
      TrezorMessage.EntropyAck
        .newBuilder()
        .setEntropy(ByteString.copyFrom(entropy))
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> signMessage(
    int account,
    KeyChain.KeyPurpose keyPurpose,
    int index,
    byte[] message
  ) {
    return sendMessage(
      TrezorMessage.SignMessage
        .newBuilder()
          // Build the chain code
        .addAllAddressN(TrezorMessageUtils.buildAddressN(account, keyPurpose, index))
        .setCoinName("Bitcoin")
        .setMessage(ByteString.copyFrom(message))
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> verifyMessage(
    Address address,
    byte[] signature,
    byte[] message
  ) {
    return sendMessage(
      TrezorMessage.VerifyMessage
        .newBuilder()
        .setAddress(address.toString())
        .setSignature(ByteString.copyFrom(signature))
        .setMessage(ByteString.copyFrom(message))
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> encryptMessage(
    byte[] pubKey,
    byte[] message,
    boolean displayOnly
  ) {
    return sendMessage(
      TrezorMessage.EncryptMessage
        .newBuilder()
        .setPubkey(ByteString.copyFrom(pubKey))
        .setMessage(ByteString.copyFrom(message))
        .setDisplayOnly(displayOnly)
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> decryptMessage(
    int account,
    KeyChain.KeyPurpose keyPurpose,
    int index,
    byte[] message
  ) {
    return sendMessage(
      TrezorMessage.DecryptMessage
        .newBuilder()
          // Build the chain code
        .addAllAddressN(TrezorMessageUtils.buildAddressN(account, keyPurpose, index))
        .setMessage(ByteString.copyFrom(message))
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> cipherKeyValue(
    int account,
    KeyChain.KeyPurpose keyPurpose,
    int index,
    byte[] key,
    byte[] keyValue,
    boolean isEncrypting,
    boolean askOnDecrypt,
    boolean askOnEncrypt
  ) {
    return sendMessage(
      TrezorMessage.CipherKeyValue
        .newBuilder()
          // Build the chain code
        .addAllAddressN(TrezorMessageUtils.buildAddressN(account, keyPurpose, index))
        .setAskOnDecrypt(askOnDecrypt)
        .setAskOnEncrypt(askOnEncrypt)
        .setEncrypt(isEncrypting)
        .setKeyBytes(ByteString.copyFrom(key))
        .setValue(ByteString.copyFrom(keyValue))
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> estimateTxSize(Transaction tx) {

    int inputsCount = tx.getInputs().size();
    int outputsCount = tx.getOutputs().size();

    return sendMessage(
      TrezorMessage.EstimateTxSize
        .newBuilder()
        .setCoinName("Bitcoin")
        .setInputsCount(inputsCount)
        .setOutputsCount(outputsCount)
        .build()
    );
  }

  /**
   * <p>Send a message to the device that should have a near-immediate (under 5 second) response.</p>
   * <p>If the response times out a FAILURE message should be generated.</p>
   *
   * @param message The message to send to the hardware wallet
   *
   * @return An optional low level message event, present only in blocking implementations
   */
  protected abstract Optional<MessageEvent> sendMessage(Message message);

  /**
   * <p>Send a message to the device with an arbitrary response duration.</p>
   * <p>If the response times out a FAILURE message should be generated.</p>
   *
   * @param message  The message to send to the hardware wallet
   * @param duration The duration to wait before returning
   * @param timeUnit The time unit
   *
   * @return An optional low level message event, present only in blocking implementations
   */
  protected abstract Optional<MessageEvent> sendMessage(Message message, int duration, TimeUnit timeUnit);

}
