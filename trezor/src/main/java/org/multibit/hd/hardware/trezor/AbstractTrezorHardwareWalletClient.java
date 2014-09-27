package org.multibit.hd.hardware.trezor;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;

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

  @Override
  public Optional<HardwareWalletProtocolEvent> initialize() {
    return sendMessage(
      TrezorMessage.Initialize
        .newBuilder()
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> ping() {
    return sendMessage(
      TrezorMessage.Ping
        .newBuilder()
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> clearSession() {
    return sendMessage(
      TrezorMessage.ClearSession
        .newBuilder()
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> changePin(boolean remove) {

    return sendMessage(
      TrezorMessage.ChangePin
        .newBuilder()
        .setRemove(remove)
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> wipeDevice() {
    return sendMessage(
      TrezorMessage.WipeDevice
        .newBuilder()
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> firmwareErase() {
    throw new UnsupportedOperationException("Use the mytrezor.com website for firmware upgrades.");
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> firmwareUpload() {
    throw new UnsupportedOperationException("Use the mytrezor.com website for firmware upgrades.");
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> getEntropy() {
    return sendMessage(
      TrezorMessage.GetEntropy
        .newBuilder()
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> getPublicKey(int index, int value, Optional<String> coinName) {

    // The master public key normally takes up to 10 seconds to complete
    return sendMessage(
      TrezorMessage.GetPublicKey
        .newBuilder()
        .setAddressN(index, value)
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> loadDevice(
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
  public Optional<HardwareWalletProtocolEvent> resetDevice(
    String language,
    String label,
    boolean displayRandom,
    boolean passphraseProtection,
    boolean pinProtection,
    int strength
  ) {

    return sendMessage(
      TrezorMessage.ResetDevice
        .newBuilder()
        .setLanguage(language)
        .setLabel(label)
        .setDisplayRandom(displayRandom)
        .setPassphraseProtection(passphraseProtection)
        .setStrength(strength)
        .setPinProtection(pinProtection)
        .build()
    );

  }

  @Override
  public Optional<HardwareWalletProtocolEvent> recoverDevice(String language, String label, int wordCount, boolean passphraseProtection, boolean pinProtection) {
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
  public Optional<HardwareWalletProtocolEvent> wordAck(String word) {
    return sendMessage(
      TrezorMessage.WordAck
        .newBuilder()
        .setWord(word)
        .build()
    );
  }

  @Override
  public Optional<Transaction> signTx(Transaction tx) {

    // TODO (GR) This is currently unmodified
    return Optional.of(tx);

  }

  @Override
  public Optional<Transaction> simpleSignTx(Transaction tx) {
    // TODO (GR) This is currently unmodified
    return Optional.of(tx);
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> pinMatrixAck(String pin) {
    return sendMessage(
      TrezorMessage.PinMatrixAck
        .newBuilder()
        .setPin(pin)
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> buttonAck() {
    return sendMessage(
      TrezorMessage.ButtonAck
        .newBuilder()
        .build()
    );
  }

  public Optional<HardwareWalletProtocolEvent> cancel() {
    return sendMessage(
      TrezorMessage.Cancel
        .newBuilder()
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> getAddress(int index, int value, boolean showDisplay) {
    return sendMessage(
      TrezorMessage.GetAddress
        .newBuilder()
        .setAddressN(index, value)
        .setCoinName("Bitcoin")
        .setShowDisplay(showDisplay)
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> applySettings(String language, String label) {

    return sendMessage(
      TrezorMessage.ApplySettings
        .newBuilder()
        .setLanguage(language)
        .setLabel(label)
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> entropyAck(byte[] entropy) {
    return sendMessage(
      TrezorMessage.EntropyAck
        .newBuilder()
        .setEntropy(ByteString.copyFrom(entropy))
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> signMessage(int index, int value, byte[] message) {
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
  public Optional<HardwareWalletProtocolEvent> verifyMessage(Address address, byte[] signature, byte[] message) {
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
  public Optional<HardwareWalletProtocolEvent> encryptMessage(byte[] pubKey, byte[] message, boolean displayOnly) {
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
  public Optional<HardwareWalletProtocolEvent> decryptMessage(int index, int value, byte[] message) {
    return sendMessage(
      TrezorMessage.DecryptMessage
        .newBuilder()
        .setAddressN(index, value)
        .setMessage(ByteString.copyFrom(message))
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> cipherKeyValue(int index, int value, byte[] key, byte[] keyValue, boolean encrypt, boolean askOnDecrypt, boolean askOnEncrypt) {
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
  public Optional<HardwareWalletProtocolEvent> passphraseAck(String passphrase) {
    return sendMessage(
      TrezorMessage.PassphraseAck
        .newBuilder()
        .setPassphrase(passphrase)
        .build()
    );
  }

  @Override
  public Optional<HardwareWalletProtocolEvent> estimateTxSize(Transaction tx) {

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
   * @param messsage The message to send to the hardware wallet
   *
   * @return An optional hardware wallet event, present only in blocking implementations
   */
  protected abstract Optional<HardwareWalletProtocolEvent> sendMessage(Message messsage);
}
