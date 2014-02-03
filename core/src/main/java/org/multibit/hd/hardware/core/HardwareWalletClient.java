package org.multibit.hd.hardware.core;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;

/**
 * <p>Interface to provide the following to applications:</p>
 * <ul>
 * <li>Common methods available to different hardware wallet devices</li>
 * </ul>
 * <p>A hardware wallet client acts as the bridge between the application and the device. It provides a common interface for sending messages
 * to the device, and subscribes to events coming from the device.</p>
 * <p>Blocking implementations will block their thread of execution until a response is received.</p>
 * <p>Hardware wallet clients are tightly coupled to their respective wallet so are typically referenced statically in consuming applications.</p>
 *
 * @since 0.0.1
 *  
 */
public interface HardwareWalletClient {

  /**
   * <p>Connect to the hardware wallet device. No initialization takes place.</p>
   */
  void connect();

  /**
   * <p>Disconnect from the hardware wallet device. This client instance can no longer be used.</p>
   */
  void disconnect();

  /**
   * <p>Send the INITIALIZE message to the device.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FEATURES containing the available feature set</li>
   * </ul>
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> initialize();

  /**
   * <p>Send the PING message to the device</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the device is present</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * </ul>
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> ping();

  /**
   * <p>Send the CHANGE_PIN message to the device. This is normally in response to receiving an PinRequest from
   * the device. Client software is expected to ask the user for their PIN in a secure manner
   * and securely erase the user input.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the password was rejected</li>
   * </ul>
   *
   * @param remove True if the PIN should be removed
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> changePin(boolean remove);

  /**
   * <p>Send the WIPE_DEVICE message to the device. The device will respond by cancelling its pending
   * action and clearing the screen.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the message was acknowledged</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * </ul>
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> wipeDevice();

  /**
   * <p>Send the FIRMWARE_ERASE message to the device.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the message was acknowledged</li>
   * </ul>
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> firmwareErase();

  /**
   * <p>Send the FIRMWARE_UPLOAD message to the device.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the message was acknowledged</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * </ul>
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> firmwareUpload();

  /**
   * <p>Send the GET_ENTROPY message to the device. The device will respond by providing some random
   * data from its internal hardware random number generator.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>ENTROPY (with data) if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> getEntropy();

  /**
   * <p>Send the LOAD_DEVICE message to the device. The device will overwrite any existing private keys and replace
   * them with based on the seed value provided.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded (may take up to 10 seconds)</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param language             The language code
   * @param seed                 The seed value provided by the user
   * @param pin                  The personal identification number (PIN) for quick confirmations (will be securely erased)
   * @param passphraseProtection True if the device should use passphrase protection
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> loadDevice(
    String language,
    String seed,
    String pin,
    boolean passphraseProtection);

  /**
   * <p>Send the RESET_DEVICE message to the device. The device will perform a full reset, generating a new seed
   * and asking the user for new settings (PIN etc).</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param language             The language code
   * @param label                The label to display
   * @param displayRandom        True if the device should display the entropy generated by the device before asking for additional
   * @param passphraseProtection True if the device should use passphrase protection
   * @param pinProtection        True if the device should use PIN protection
   * @param strength             The strength of the seed in bits (default is 128)
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> resetDevice(
    String language,
    String label,
    boolean displayRandom,
    boolean passphraseProtection,
    boolean pinProtection,
    int strength
  );

  /**
   * <p>Send the RECOVER_DEVICE message to the device to start the workflow of asking user for specific words from their seed</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>WORD_REQUEST for each word in the user seed</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param language             The language code
   * @param label                The label to display
   * @param wordCount            The number of words in the seed
   * @param passphraseProtection True if the device should use passphrase protection
   * @param pinProtection        True if the device should use PIN protection
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> recoverDevice(
    String language,
    String label,
    int wordCount,
    boolean passphraseProtection,
    boolean pinProtection
  );

  /**
   * <p>Send the RESET_DEVICE message to the device. The device will perform a full reset, generating a new seed
   * and asking the user for new settings (PIN etc).</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param language             The language code
   * @param label                The label to display
   * @param wordCount            The number of words in the seed
   * @param passphraseProtection True if the device should use passphrase protection
   * @param pinProtection        True if the device should use PIN protection
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> wordAck(
    String language,
    String label,
    int wordCount,
    boolean passphraseProtection,
    boolean pinProtection
  );

  /**
   * <p>Send the SIGN_TX message to the device. Behind the scenes the device will response with a series of TxRequests
   * in order to  build up the overall transaction. This client takes care of all the chatter leaving a final
   * response condition.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param tx The Bitcoinj transaction providing all the necessary information (will be modified)
   *
   * @return The signed transaction from the device (if present)
   */
  public Optional<Transaction> signTx(Transaction tx);

  /**
   * <p>Send the PIN_MATRIX_ACK message to the device in response to a PIN_MATRIX_REQUEST.</p>
   * <p>Implementers are expected to show a PIN matrix on the UI.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param pin The PIN as entered from the PIN matrix (obfuscated)
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> pinMatrixAck(byte[] pin);

  /**
   * <p>Send the CANCEL message to the device in response to a BUTTON_REQUEST, PIN_MATRIX_REQUEST or PASSPHRASE_REQUEST. </p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> cancel();

  /**
   * <p>Send the BUTTON_ACK message to the device in response to a BUTTON_REQUEST. The calling software will
   * then block (appropriately for its UI) until the device responds/fails.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>BUTTON_REQUEST if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> buttonAck();

  /**
   * <p>Send the APPLY_SETTINGS message to the device.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param language The language code
   * @param label    The label
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> applySettings(String language, String label);

  /**
   * <p>Send the GET_ADDRESS message to the device. The device will respond by providing an address calculated
   * based on the <a href="https://en.bitcoin.it/wiki/BIP_0032">BIP 0032</a> deterministic wallet approach from
   * the master node.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>ADDRESS (with data) if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param index    The index of the chain node from the master node
   * @param value    The index of the address from the given chain node
   * @param coinName The optional coin name with a default of "Bitcoin"
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> getAddress(int index, int value, Optional<String> coinName);

  /**
   * <p>Send the GET_PUBLIC_KEY message to the device. The device will respond by providing a public key calculated
   * based on the <a href="https://en.bitcoin.it/wiki/BIP_0032">BIP 0032</a> deterministic wallet approach from
   * the master node.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>PUBLIC_KEY if the operation succeeded (may take up to 10 seconds)</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param index    The index of the chain node from the master node
   * @param value    The index of the address from the given chain node
   * @param coinName The optional coin name with a default of "Bitcoin"
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> getPublicKey(int index, int value, Optional<String> coinName);

  /**
   * <p>Send the ENTROPY_ACK message to the device in response to an ENTROPY_REQUEST.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> entropyAck();

  /**
   * <p>Send the SIGN_MESSAGE message to the device.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>MESSAGE_SIGNATURE if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> signMessage();

  /**
   * <p>Send the VERIFY_MESSAGE message to the device.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>MESSAGE_SIGNATURE if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param address   The address to use for verification
   * @param signature The signature
   * @param message   The message to sign
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> verifyMessage(Address address, byte[] signature, String message);

  /**
   * <p>Send the PASSPHRASE_ACK message to the device in response to a PASSPHRASE_REQUEST.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>SUCCESS if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> passphraseAck();

  /**
   * <p>Send the ESTIMATE_TX_SIZE message to the device.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>TX_SIZE if the operation succeeded</li>
   * <li>PIN_MATRIX_REQUEST if the PIN is needed</li>
   * <li>FAILURE if the operation was unsuccessful</li>
   * </ul>
   *
   * @param tx The Bitcoinj transaction providing all the necessary information (will not be modified)
   *
   * @return The response if implementation is blocking. Absent if non-blocking or device failure.
   */
  Optional<HardwareWalletProtocolEvent> estimateTxSize(Transaction tx);

  /**
   * @param event The hardware wallet protocol event
   */
  @Subscribe
  void onHardwareWalletProtocolEvent(HardwareWalletProtocolEvent event);
}
