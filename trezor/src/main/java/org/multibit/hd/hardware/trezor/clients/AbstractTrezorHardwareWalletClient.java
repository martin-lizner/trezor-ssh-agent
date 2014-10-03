package org.multibit.hd.hardware.trezor.clients;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.params.MainNetParams;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.messages.TxRequest;
import org.multibit.hd.hardware.core.utils.TransactionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public Optional<MessageEvent> changePin(boolean remove) {

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

  @Override
  public Optional<MessageEvent> getPublicKey(int index, int value, Optional<String> coinName) {

    // The master public key normally takes up to 10 seconds to complete
    return sendMessage(
      TrezorMessage.GetPublicKey
        .newBuilder()
        .setAddressN(index, value)
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> loadDevice(
    String language,
    String seed,
    String pin,
    boolean passphraseProtection
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
    return sendMessage(TrezorMessage.LoadDevice
        .newBuilder()
        .setMnemonic(seed)
        .setLanguage(language)
        .setNode(nodeType)
        .setPin(pin)
        .setPassphraseProtection(passphraseProtection)
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
  public Optional<MessageEvent> recoverDevice(String language, String label, int wordCount, boolean passphraseProtection, boolean pinProtection) {
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

    // TODO (GR) Implement the transaction decode

    return sendMessage(
      TrezorMessage.SimpleSignTx
        .newBuilder()
        .setCoinName("Bitcoin")
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> txAck(TxRequest txRequest, Transaction tx) {

    final TrezorType.TransactionType txType;

    // Get the index
    int txIndex;

    // Get the transaction hash if present
    Optional<byte[]> txHash = txRequest.getTxRequestDetailsType().getTxHash();

    // Assume we're working with the current (child) transaction to start with
    Optional<Transaction> requestedTx = Optional.of(tx);

    // Check if the requested transaction is different to the current
    if (txHash.isPresent()) {
      // Need to look up a transaction by hash
      requestedTx = TransactionUtils.getTransactionByHash(tx, txHash.get());

      // Check if the transaction was found
      if (!requestedTx.isPresent()) {
        log.error("Device requested unknown hash: {}", txHash);
        throw new IllegalArgumentException("Device requested unknown hash.");
      }

    }

    // Have the required transaction at this point

    // TODO Extract method into utility class TrezorMessageUtils
    switch (txRequest.getTxRequestType()) {
      case TX_META:

        // Provide details about the requested transaction
        txType = TrezorType.TransactionType
          .newBuilder()
          .setVersion((int) requestedTx.get().getVersion())
          .setLockTime((int) requestedTx.get().getLockTime())
            // No support for binary outputs at the moment
          .setInputsCnt(requestedTx.get().getInputs().size())
          .setOutputsCnt(requestedTx.get().getOutputs().size())
          .build();

        break;
      case TX_INPUT:

        // Get the transaction input indicated by the request index
        txIndex = txRequest.getTxRequestDetailsType().getIndex();
        TransactionInput input = requestedTx.get().getInput(txIndex);

        // Check for connectivity
        if (input.getConnectedOutput() == null) {
          log.error("Input {} does not have a connected output", txIndex);
          throw new IllegalArgumentException("Input " + txIndex + " does not have a connected output.");
        }

        // Must be OK to be here
        int prevIndex = input.getConnectedOutput().getIndex();
        byte[] prevHash = input.getParentTransaction().getHash().getBytes();

        // No multisig support in MBHD yet
        TrezorType.InputScriptType inputScriptType = TrezorType.InputScriptType.SPENDADDRESS;

        TrezorType.TxInputType txInputType = TrezorType.TxInputType
          .newBuilder()
            // TODO Require receiving Address -> index,value co-ordinates adapter
          .addAddressN(txIndex)
          .addAddressN(0)
          .setScriptType(inputScriptType)
          .setPrevIndex(prevIndex)
          .setPrevHash(ByteString.copyFrom(prevHash))
          .build();

        txType = TrezorType.TransactionType
          .newBuilder()
          .addInputs(txInputType)
          .build();

        break;
      case TX_OUTPUT:
        // Get the transaction output indicated by the request index
        txIndex = txRequest.getTxRequestDetailsType().getIndex();
        TransactionOutput output = tx.getOutput(txIndex);

        // No P2SH at the moment
        Address address = output.getAddressFromP2PKHScript(MainNetParams.get());
        if (address == null) {
          throw new IllegalArgumentException("TxOutput " + txIndex + " has no address.");
        }

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

        txType = TrezorType.TransactionType
          .newBuilder()
          .addOutputs(txOutputType)
          .build();
        break;
      case TX_FINISHED:
        // Do not send message
        log.info("TxSign workflow complete.");
        return Optional.absent();
      default:
        log.error("Unknown TxReturnType: {}", txRequest.getTxRequestType().name());
        return Optional.absent();
    }

    return sendMessage(
      TrezorMessage.TxAck
        .newBuilder()
        .setTx(txType)
        .build()
    );

  }

  @Override
  public Optional<MessageEvent> pinMatrixAck(String pin) {
    return sendMessage(
      TrezorMessage.PinMatrixAck
        .newBuilder()
        .setPin(pin)
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> buttonAck() {
    return sendMessage(
      TrezorMessage.ButtonAck
        .newBuilder()
        .build(),
      // Allow user time to decide
      10, TimeUnit.MINUTES
    );
  }

  public Optional<MessageEvent> cancel() {
    return sendMessage(
      TrezorMessage.Cancel
        .newBuilder()
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> getAddress(int index, int value, boolean showDisplay) {
    return sendMessage(
      TrezorMessage.GetAddress
        .newBuilder()
        .addAddressN(index)
        .addAddressN(value)
        .setCoinName("Bitcoin")
        .setShowDisplay(showDisplay)
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
  public Optional<MessageEvent> signMessage(int index, int value, byte[] message) {
    return sendMessage(
      TrezorMessage.SignMessage
        .newBuilder()
        .setAddressN(index, value)
        .setCoinName("Bitcoin")
        .setMessage(ByteString.copyFrom(message))
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> verifyMessage(Address address, byte[] signature, byte[] message) {
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
  public Optional<MessageEvent> encryptMessage(byte[] pubKey, byte[] message, boolean displayOnly) {
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
  public Optional<MessageEvent> decryptMessage(int index, int value, byte[] message) {
    return sendMessage(
      TrezorMessage.DecryptMessage
        .newBuilder()
        .setAddressN(index, value)
        .setMessage(ByteString.copyFrom(message))
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> cipherKeyValue(int index, int value, byte[] key, byte[] keyValue, boolean encrypt, boolean askOnDecrypt, boolean askOnEncrypt) {
    return sendMessage(
      TrezorMessage.CipherKeyValue
        .newBuilder()
        .setAddressN(index, value)
        .setAskOnDecrypt(askOnDecrypt)
        .setAskOnEncrypt(askOnEncrypt)
        .setEncrypt(encrypt)
        .setKeyBytes(ByteString.copyFrom(key))
        .setValue(ByteString.copyFrom(keyValue))
        .build()
    );
  }

  @Override
  public Optional<MessageEvent> passphraseAck(String passphrase) {
    return sendMessage(
      TrezorMessage.PassphraseAck
        .newBuilder()
        .setPassphrase(passphrase)
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
   * <p>Send a message to the device that should have a near-immediate (under 1 second) response.</p>
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
   * @param message@return An optional low level message event, present only in blocking implementations
   */
  protected abstract Optional<MessageEvent> sendMessage(Message message, int duration, TimeUnit timeUnit);

}
