package org.multibit.hd.hardware.core.clients;

import com.google.bitcoin.core.Transaction;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import org.multibit.hd.hardware.core.events.HardwareWalletProtocolEvent;

/**
 * <p>Interface to provide the following to applications:</p>
 * <ul>
 * <li>Common methods available to different hardware wallet devices</li>
 * </ul>
 * <p>Please refer to the {@link org.multibit.hd.hardware.core.clients.NonBlockingHardwareWalletClient} if making
 * changes to this interface</p>
 * <p>A hardware wallet client acts as the bridge between the application and the device. It provides a common interface for sending messages
 * to the device, and subscribes to events coming from the device.</p>
 * <p>Blocking implementations will block their thread of execution until a response is received.</p>
 *
 * <p>Other parts of the </p>
 *
 * @since 0.0.1
 *  
 */
public interface BlockingHardwareWalletClient {

  /**
   * <p>Connect to the hardware wallet device. No initialization takes place.</p>
   */
  void connect();

  /**
   * <p>Disconnect from the hardware wallet device. This client instance can no longer be used.</p>
   */
  void disconnect();

  /**
   * <p>Send the Ping message to the device</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the device is present</li>
   * </ul>
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent ping();

  /**
   * <p>Send the Initialize message to the device. This initiates the session using a unique session ID for this
   * client instance.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Features containing the available feature set</li>
   * </ul>
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent initialize();

  /**
   * <p>Send the GetUUID message to the device. The device will respond with its unique identifier.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>UUID containing the unique identifier for the device</li>
   * </ul>
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent getUUID();

  /**
   * <p>Send the OtpAck message to the device. This is normally in response to receiving an OtpRequest from
   * the device. Client software is expected to ask the user for their one-time password in a secure manner
   * and securely erase the user input.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the password was accepted</li>
   * <li>Failure if the password was rejected</li>
   * </ul>
   * <p>Please refer to <a href="http://stackoverflow
   * .com/questions/8881291/why-is-char-preferred-over-string-for-passwords/8881376#8881376">this Stack
   * Overflow answer by Jon Skeet</a> regarding the choice of <code>char[]</code> for the argument.</p>
   * <p>This method will erase the array argument in a reasonably secure manner. It is not immune to memory
   * monitoring and this password will be provided in the clear across the wire,
   * but if the attacker only has limited access to system resources it raises the bar considerably.</p>
   *
   * @param oneTimePassword The one-time password from the user (will be securely erased)
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent optAck(char[] oneTimePassword);

  /**
   * <p>Send the OtpCancel message to the device. The device will respond by cancelling its pending
   * action and clearing the screen.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the message was acknowledged</li>
   * </ul>
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent optCancel();

  /**
   * <p>Send the OtpAck message to the device. This is normally in response to receiving an PinRequest from
   * the device. Client software is expected to ask the user for their PIN in a secure manner
   * and securely erase the user input.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the password was accepted</li>
   * <li>Failure if the password was rejected</li>
   * </ul>
   *
   * @param pin The personal identification number (PIN) from the user (will be securely erased)
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent pinAck(char[] pin);

  /**
   * <p>Send the PinCancel message to the device. The device will respond by cancelling its pending
   * action and clearing the screen.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the message was acknowledged</li>
   * </ul>
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent pinCancel();

  /**
   * <p>Send the GetEntropy message to the device. The device will respond by providing some random
   * data from its internal hardware random number generator.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Entropy (with data) if the operation succeeded</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent getEntropy();

  /**
   * <p>Send the SetMaxFeeKb message to the device. The device will respond by updating its maximum
   * fee to that provided. This level is used to provide internal sanity checking during transaction signing
   * to ensure that an appropriate fee is being charged (which can vary with the exchange rate).</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the operation succeeded</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @param satoshisPerKb The number of Satoshis per Kb (with some sanity checking)
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent setMaxFeeKb(long satoshisPerKb);

  /**
   * <p>Send the GetMasterPublicKey message to the device. The device will respond by providing the
   * master public key which can be used to generate other public keys.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>MasterPublicKey if the operation succeeded (may take up to 10 seconds)</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent getMasterPublicKey();

  /**
   * <p>Send the GetAddress message to the device. The device will respond by providing an address calculated
   * based on the <a href="https://en.bitcoin.it/wiki/BIP_0032">BIP 0032</a> deterministic wallet approach.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Address (with data) if the operation succeeded</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @param index The index position of the generated external public key (account i from BIP 0032)
   * @param value The key pair number (k'th key from BIP 0032)
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent getAddress(int index, int value);

  /**
   * <p>Send the LoadDevice message to the device. The device will overwrite any existing private keys and replace
   * them with based on the seed value provided.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the operation succeeded (may take up to 10 seconds)</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @param seed   The seed value provided by the user
   * @param useOtp True if the device should use a one-time password (OTP)
   * @param pin    The personal identification number (PIN) for quick confirmations (will be securely erased)
   * @param useSpv True if the device should use Simplified Payment Verification (SPV) from the main computer to
   *               verify transactions
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent loadDevice(
    char[] seed,
    boolean useOtp,
    byte[] pin,
    boolean useSpv);

  /**
   * <p>Send the ResetDevice message to the device. The device will perform a full reset, generating a new seed
   * and asking the user for new settings.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the operation succeeded</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @param entropy Additional entropy for use when generating the new seed. Use at least 256 bytes from <a
   *                href="https://en.wikipedia.org/wiki/Urandom">dev/random</a>. The array will be securely erased
   *                after use.
   *
   * @return The response from the device
   */
  HardwareWalletProtocolEvent resetDevice(byte[] entropy);

  /**
   * <p>Send the SignTx message to the device. Behind the scenes the device will response with a series of TxRequests
   * in order to  build up the overall transaction. This client takes care of all the chatter leaving a final
   * response condition.</p>
   * <p>Expected response events are:</p>
   * <ul>
   * <li>Success if the operation succeeded</li>
   * <li>OptRequest if the one-time password is needed</li>
   * <li>PinRequest if the PIN is needed</li>
   * <li>Failure if the operation was unsuccessful</li>
   * </ul>
   *
   * @param tx The Bitcoinj transaction providing all the necessary information (will be modified)
   *
   * @return The signed transaction from the device (if present)
   */
  public Optional<Transaction> signTx(Transaction tx);

  /**
   * @param event The hardware wallet protocol event
   */
  @Subscribe
  void onHardwareWalletProtocolEvent(HardwareWalletProtocolEvent event);

  /**
   * @param size The number of bytes of random data required
   *
   * @return The random bytes (based on SecureRandom implementation)
   */
  public byte[] newEntropy(int size);
}
