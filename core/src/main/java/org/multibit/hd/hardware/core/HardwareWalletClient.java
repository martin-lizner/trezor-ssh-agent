package org.multibit.hd.hardware.core;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.wallet.KeyChain;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.messages.TxRequest;
import org.multibit.hd.hardware.core.wallets.Connectable;

import java.util.List;
import java.util.Map;

/**
 * <p>Interface to provide the following to applications:</p>
 * <ul>
 * <li>Common methods available to different hardware wallet devices</li>
 * </ul>
 * <p>A hardware wallet client acts as the bridge between the application and the device. It provides a common interface for sending messages
 * to the device, and subscribes to events coming from the device.</p>
 * <p>Blocking implementations will block their thread of execution until a response is received.</p>
 * <p>Hardware wallet clients are tightly coupled to their respective wallet so are typically referenced statically in consuming applications.</p>
 */
public interface HardwareWalletClient extends Connectable {

  /**
   * <p>Reset device to default state and ask for device details</p>
   * <p>Send the INITIALISE message to the device.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FEATURES containing the available feature set</li>
   * </ul>
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> initialise();

  /**
   * <p>Send the PING message to the device</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the device is present</li>
   * </ul>
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> ping();

  /**
   * <p>Send the CLEAR_SESSION message to the device.</p>
   * <p>Clear session (removes cached PIN, passphrase, etc).</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the PIN is needed</li>
   * </ul>
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> clearSession();

  /**
   * <p>Send the CHANGE_PIN message to the device. The device will respond by showing a secure PIN selection
   * screen and asking the user to verify accordingly.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>BUTTON_REQUEST if a button press is needed</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the password was rejected</li>
   * </ul>
   *
   * @param remove True if the PIN should be removed
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> changePIN(boolean remove);

  /**
   * <p>Send the WIPE_DEVICE message to the device. The device will respond by cancelling its pending
   * action and clearing the screen. It will also wipe all sensitive data and settings back to factory defaults.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the message was acknowledged</li>
   * <li>BUTTON_REQUEST if a button press is needed</li>
   * </ul>
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> wipeDevice();

  /**
   * <p>Send the FIRMWARE_ERASE message to the device.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the message was acknowledged</li>
   * </ul>
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> firmwareErase();

  /**
   * <p>Send the FIRMWARE_UPLOAD message to the device.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the message was acknowledged</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * </ul>
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> firmwareUpload();

  /**
   * <p>Send the ENTROPY message to the device. The device will respond by providing some random
   * data from its internal hardware random number generator.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>ENTROPY (with data) if the operation succeeded</li>
   * <li>BUTTON_REQUEST if a button press is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> getEntropy();

  /**
   * <p>Send the LOAD_DEVICE message to the device. The device will overwrite any existing private keys and replace
   * them based on the seed phrase provided.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded (may take up to 10 seconds)</li>
   * <li>BUTTON_REQUEST if a button press is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param language   The language code
   * @param label      The label to display below the logo (e.g "Fred")
   * @param seedPhrase The seed phrase provided by the user in the clear
   * @param pin        The personal identification number (PIN) in the clear
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> loadDevice(
    String language,
    String label,
    String seedPhrase,
    String pin
  );

  /**
   * <p>Send the RESET_DEVICE message to the device. The device will perform a full reset, generating a new seed
   * and asking the user for new settings (PIN etc). This is typically needed when a device is first un-boxed.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>ENTROPY_REQUEST if the user should supply additional entropy (recommended)</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param language      The language code (e.g. "english")
   * @param label         The label to display below the logo (e.g "Fred")
   * @param displayRandom True if the device should display the entropy generated by the device before asking for additional entropy
   * @param pinProtection True if the device should use PIN protection
   * @param strength      The strength of the seed in bits (default is 128)
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> resetDevice(
    String language,
    String label,
    boolean displayRandom,
    boolean pinProtection,
    int strength
  );

  /**
   * <p>Send the RECOVER_DEVICE message to the device to start the workflow of asking user for specific words from their seed.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>WORD_REQUEST for each word in the user seed</li>
   * </ul>
   *
   * @param language             The language code
   * @param label                The label to display
   * @param wordCount            The number of words in the seed
   * @param passphraseProtection True if the device should use passphrase protection
   * @param pinProtection        True if the device should use PIN protection
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> recoverDevice(
    String language,
    String label,
    int wordCount,
    boolean passphraseProtection,
    boolean pinProtection
  );

  /**
   * <p>Send the WORD_ACK message to the device containing a word from the seed phrase.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the overall operation succeeded</li>
   * <li>WORD_REQUEST if another word is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param word A word from the seed phrase
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> wordAck(String word);

  /**
   * <p>Send the SIGN_TX message to the device. Behind the scenes the device will response with a series of TxRequests
   * in order to  build up the overall transaction. This client takes care of all the chatter leaving a final
   * response condition. This allows transactions of arbitrary size to be created.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>PASSPHRASE_REQUEST if the passphrase is needed</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param tx The Bitcoinj transaction providing all the necessary information (will be modified)
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  public Optional<MessageEvent> signTx(Transaction tx);

  /**
   * <p>Send the SIMPLE_SIGN_TX message to the device. This will use the supplied transaction object and attempt
   * to deliver it to the device in a single request. This limits the size of the transaction but simplifies the
   * call.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>PASSPHRASE_REQUEST if the passphrase is needed</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param tx The Bitcoinj transaction providing all the necessary information (will be modified)
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  public Optional<MessageEvent> simpleSignTx(Transaction tx);

  /**
   * <p>Send the TX_ACK message to the device. This contains a description of an input or output depending
   * on the contents of an earlier TX_REQUEST message.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>TX_REQUEST if more transaction information is required or to notify completion of workflow</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param txRequest               The transaction request describing what is required
   * @param tx                      The Bitcoinj transaction providing all the necessary information (will be modified)
   * @param receivingAddressPathMap The receiving address path map (keyed on input index and providing deterministic path to receiving address)
   * @param changeAddressPathMap    The change address path map (keyed on Address and providing deterministic path to change address)
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> txAck(TxRequest txRequest, Transaction tx, Map<Integer, ImmutableList<ChildNumber>> receivingAddressPathMap, Map<Address, ImmutableList<ChildNumber>> changeAddressPathMap);

  /**
   * <p>Send the PIN_MATRIX_ACK message to the device in response to a PIN_MATRIX_REQUEST.</p>
   * <p>Implementers are expected to show a PIN matrix on the UI.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param pin The PIN as entered from the PIN matrix (obfuscated)
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> pinMatrixAck(String pin);

  /**
   * <p>Send the CANCEL message to the device in response to a BUTTON_REQUEST, PIN_MATRIX_REQUEST or PASSPHRASE_REQUEST. </p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>BUTTON_REQUEST if a button press is needed</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> cancel();

  /**
   * <p>Send the BUTTON_ACK message to the device in response to a BUTTON_REQUEST. The calling software will
   * then block (appropriately for its UI) until the device responds/fails.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>BUTTON_REQUEST if the operation succeeded</li>
   * </ul>
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> buttonAck();

  /**
   * <p>Send the APPLY_SETTINGS message to the device.</p>
   * <p>Change language and/or label of the device</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>BUTTON_REQUEST if a button press is needed</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param language The language code
   * @param label    The label
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> applySettings(String language, String label);

  /**
   * <p>Send the GET_ADDRESS message to the device. The device will respond by providing an address calculated
   * based on the <a href="https://en.bitcoin.it/wiki/BIP_0044">BIP-44</a> deterministic wallet approach from
   * the master node.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>ADDRESS (with data) if the operation succeeded</li>
   * <li>PASSPHRASE_REQUEST if the passphrase is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param account     The plain account number (0 gives maximum compatibility)
   * @param keyPurpose  The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index       The plain index of the required address
   * @param showDisplay True if the device should display the same address to allow the user to verify no tampering has occurred (recommended).
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  public Optional<MessageEvent> getAddress(int account, KeyChain.KeyPurpose keyPurpose, int index, boolean showDisplay);

  /**
   * <p>Send the GET_PUBLIC_KEY message to the device. The device will respond by providing a public key calculated
   * based on the <a href="https://en.bitcoin.it/wiki/BIP_0044">BIP-44</a> deterministic wallet approach from
   * the master node.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>PUBLIC_KEY if the operation succeeded (may take up to 10 seconds)</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param account    The plain account number (0 gives maximum compatibility)
   * @param keyPurpose The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index      The plain index of the required address
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> getPublicKey(int account, KeyChain.KeyPurpose keyPurpose, int index);

  /**
   * <p>Send the GET_PUBLIC_KEY message to the device based on the given list of child numbers. The device will respond by
   * providing an extended public key (xpub) calculated based on the <a href="https://en.bitcoin.it/wiki/BIP_0044">BIP-44</a>
   * deterministic wallet approach from the master node. If the </p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>PUBLIC_KEY if the operation succeeded (may take up to 10 seconds)</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param childNumbers The list of child numbers to explore (some of which may be hardened)
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> getDeterministicHierarchy(List<ChildNumber> childNumbers);

  /**
   * <p>Send the ENTROPY_ACK message to the device in response to an ENTROPY_REQUEST. This allows the device to obtain
   * additional entropy to offset potentially compromised hardware.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>BUTTON_REQUEST if the a button press is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param entropy The additional entropy
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> entropyAck(byte[] entropy);

  /**
   * <p>Send the SIGN_MESSAGE message to the device containing a message to sign.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>MESSAGE_SIGNATURE if the operation succeeded</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> signMessage(int account, KeyChain.KeyPurpose keyPurpose, int index, byte[] message);

  /**
   * <p>Send the VERIFY_MESSAGE message to the device containing a signed message to verify.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param address   The address to use for verification
   * @param signature The signature
   * @param message   The message to sign
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> verifyMessage(Address address, byte[] signature, byte[] message);

  /**
   * <p>Send the ENCRYPT_MESSAGE message to the device containing a message to encrypt.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param pubKey      The public key to use for encryption
   * @param message     The message to encrypt
   * @param displayOnly True if the output is for display only
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> encryptMessage(byte[] pubKey, byte[] message, boolean displayOnly);

  /**
   * <p>Send the DECRYPT_MESSAGE message to the device containing a message to decrypt.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param account    The plain account number (0 gives maximum compatibility)
   * @param keyPurpose The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index      The plain index of the required address
   * @param message    The message to decrypt
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> decryptMessage(int account, KeyChain.KeyPurpose keyPurpose, int index, byte[] message);

  /**
   * <p>Send the CIPHER_KEY_VALUE_MESSAGE message to the device containing a key. The device will encrypt or decrypt the value
   * of the given key.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param account      The plain account number (0 gives maximum compatibility)
   * @param keyPurpose   The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index        The plain index of the required address
   * @param key          The cipher key (e.g. "Some text")
   * @param keyValue     The key value (e.g. "[16 bytes of random data]")
   * @param isEncrypting True if encrypting
   * @param askOnDecrypt True if device should ask on decrypting
   * @param askOnEncrypt True if device should ask on encrypting
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> cipherKeyValue(
    int account,
    KeyChain.KeyPurpose keyPurpose,
    int index,
    byte[] key,
    byte[] keyValue,
    boolean isEncrypting,
    boolean askOnDecrypt,
    boolean askOnEncrypt
  );

  /**
   * <p>Send the ESTIMATE_TX_SIZE message to the device to estimate the size of the transaction. This behaves
   * exactly like the SignTx in that it uses the TxRequest mechanism to describe the transaction.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>TX_SIZE if the operation succeeded</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param tx The Bitcoinj transaction providing all the necessary information (will not be modified)
   *
   * @return The response event if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<MessageEvent> estimateTxSize(Transaction tx);


}
