package org.multibit.hd.hardware.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.wallet.KeyChain;
import org.multibit.hd.hardware.core.concurrent.SafeExecutors;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvents;
import org.multibit.hd.hardware.core.fsm.CreateWalletSpecification;
import org.multibit.hd.hardware.core.fsm.HardwareWalletContext;
import org.multibit.hd.hardware.core.fsm.LoadWalletSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>Service to provide the following to application:</p>
 * <ul>
 * <li>Main entry point for downstream API consumers</li>
 * <li>Handles high level hardware wallet use cases (e.g. "create new wallet" etc)</li>
 * </ul>
 *
 * <p>Refer to the examples for how to correctly configure the service for use in downstream
 * consumer applications.</p>
 *
 * @since 0.0.1
 *  
 */
public class HardwareWalletService {

  private static final Logger log = LoggerFactory.getLogger(HardwareWalletService.class);

  /**
   * Monitors the hardware client to manage state transitions in response to incoming messages
   */
  private final ListeningScheduledExecutorService clientMonitorService = SafeExecutors.newSingleThreadScheduledExecutor("monitor-hw-client");

  /**
   * The current hardware wallet context
   */
  private final HardwareWalletContext context;

  /**
   * True if the service has stopped
   */
  private boolean stopped = false;

  /**
   * @param client The hardware wallet client providing the low level messages
   */
  public HardwareWalletService(HardwareWalletClient client) {

    Preconditions.checkNotNull(client, "'client' must be present");

    context = new HardwareWalletContext(client);
  }

  /**
   * <p>Start the service and await the connection of a hardware wallet</p>
   */
  public void start() {

    if (stopped) {
      throw new IllegalStateException("Once stopped the service must be started with a fresh instance");
    }

    // Start the hardware wallet state machine
    clientMonitorService.scheduleWithFixedDelay(
      new Runnable() {
        @Override
        public void run() {

          // Note: If an unhandled error occurs in a scheduled exception
          // it causes all future requests to be suppressed
          // We want to alert the user to a failure and keep active
          // to avoid "silent failures"

          try {

            // It if we are in the await state then we use a client
            // call (e.g. initialise()) to poke the device to elicit
            // a low level message response
            context.getState().await(context);
          } catch (RuntimeException e) {
            log.error("Unexpected error transitioning between states", e);
            // Trigger a failure mode
            context.resetToFailed();
          }

        }
      },
      0, // Immediate start
      // Delay time in order to progress the state
      // Devices will respond with some kind of event within 1 second for states that are
      // awaiting progression (e.g. Connected -> Initialised)
      // Setting this lower has no effect on speed of operations and may introduce
      // instability with overlapping calls due to "impatience"
      1, TimeUnit.SECONDS
    );
  }

  /**
   * <p>Stop the service</p>
   */
  public void stopAndWait() {

    log.debug("Service {} stopping...", this.getClass().getSimpleName());

    context.resetToStopped();

    // Ensure downstream subscribers are purged
    HardwareWalletEvents.unsubscribeAll();
    MessageEvents.unsubscribeAll();

    try {
      clientMonitorService.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.warn("Client monitor thread did not terminate within the allowed time");
    }

    stopped = true;

  }

  /**
   * @return True if the service is stopped
   */
  public boolean isStopped() {

    return stopped;
  }

  /**
   * @return The hardware wallet context providing access to the current device state
   */
  public HardwareWalletContext getContext() {
    return context;
  }

  /**
   * @return True if the hardware wallet has been attached and a successful connection made
   */
  public boolean isDeviceReady() {

    return context.getFeatures().isPresent();

  }

  /**
   * @return True if the hardware wallet has been initialised with a seed phrase (<code>Features.isInitialised()</code>)
   *
   * @throws IllegalStateException If called when the device is not ready (see <code>isDeviceReady()</code>)
   */
  public boolean isWalletPresent() {

    if (!context.getFeatures().isPresent()) {
      throw new IllegalStateException("Device is not ready. Check the hardware wallet events.");
    }

    return context.getFeatures().get().isInitialized();

  }

  /**
   * <p>Cancel the current operation and return to the initialised state</p>
   *
   * <p>This will trigger a SHOW_OPERATION_FAILED and a DEVICE_READY event during the reset phase</p>
   */
  public void requestCancel() {

    // Let the state changes occur as a result of the internal messages
    context.getClient().cancel();

  }

  /**
   * <p>Clear the device back to factory settings</p>
   */
  public void wipeDevice() {

    // Set the FSM context
    context.beginWipeDeviceUseCase();

  }

  /**
   * <p>Change or remove the device PIN.</p>
   *
   * @param remove True if an existing PIN should be removed
   */
  public void changePIN(boolean remove) {

    // Set the FSM context
    context.beginChangePIN(remove);
  }

  /**
   * <p>Initiate the process where the hardware wallet is first wiped then reset using its own entropy</p>
   * <p>This is the recommended method to use for creating a wallet securely.</p>
   *
   * @param language      The language (e.g. "english")
   * @param label         The label to display below the logo (e.g "Fred")
   * @param displayRandom True if the device should display the entropy generated by the device before asking for additional entropy
   * @param pinProtection True if the device should use PIN protection
   * @param strength      The number of bits in the seed phrase (128 bits = 12 words, 196 bits = 18 words, 256 bits = 24 words)
   */
  public void secureCreateWallet(
    String language,
    String label,
    boolean displayRandom,
    boolean pinProtection,
    int strength
  ) {

    // Create the specification
    CreateWalletSpecification specification = new CreateWalletSpecification(
      language,
      label,
      displayRandom,
      pinProtection,
      strength
    );

    // Set the FSM context
    context.beginCreateWallet(specification);

  }

  /**
   * <p>Initiate the process where the hardware wallet is first wiped then loaded using an external seed phrase</p>
   * <h3>This is an insecure method for creating a wallet. DO NOT USE IN PRODUCTION.</h3>
   *
   * @param language   The language (e.g. "english")
   * @param label      The label to display below the logo (e.g "Fred")
   * @param seedPhrase The seed phrase provided by the user in the clear
   * @param pin        The personal identification number (PIN) in the clear
   */
  public void loadWallet(
    String language,
    String label,
    String seedPhrase,
    String pin
  ) {

    // Create the specification
    LoadWalletSpecification specification = new LoadWalletSpecification(
      language,
      label,
      seedPhrase,
      pin
    );

    // Set the FSM context
    context.beginLoadWallet(specification);

  }

  /**
   * <p>Provide the user entered PIN</p>
   *
   * @param pin The PIN taken from the user ideally through an obfuscated PIN matrix approach
   */
  public void providePIN(String pin) {

    // Use the FSM context to decide the appropriate continuation point
    switch (context.getCurrentUseCase()) {
      case DETACHED:
        break;
      case CREATE_WALLET:
        context.continueCreateWallet_PIN(pin);
        break;
      case SIMPLE_SIGN_TX:
        context.continueSignTx_PIN(pin);
        break;
      case SIGN_TX:
        context.continueSignTx_PIN(pin);
        break;
      case REQUEST_PUBLIC_KEY:
        context.continueGetPublicKeyUseCase_PIN(pin);
        break;
      case REQUEST_DETERMINISTIC_HIERARCHY:
        context.continueGetDeterministicHierarchyUseCase_PIN(pin);
        break;
      case REQUEST_CIPHER_KEY:
        context.continueCipherKey_PIN(pin);
        break;
      case SIGN_MESSAGE:
        context.continueSignMessage_PIN(pin);
        break;
      case CHANGE_PIN:
        context.continueChangePIN_PIN(pin);
        break;
      default:
        log.warn("Unknown PIN request use case: {}", context.getCurrentUseCase().name());
    }
  }

  /**
   * <p>Provide additional entropy to the device to reduce risk of hardware compromise</p>
   *
   * @param entropy Random bytes provided by a secure random number generator (see {@link #generateEntropy()}
   */
  public void provideEntropy(byte[] entropy) {

    // Set the FSM context
    context.continueCreateWallet_Entropy(entropy);

  }

  /**
   * <p>Request an address from the device. The device will respond by providing an address calculated
   * based on the <a href="https://en.bitcoin.it/wiki/BIP_0044">BIP-44</a> deterministic wallet approach from
   * the master node.</p>
   *
   * <p>The BIP-44 chain code is arranged as follows:</p>
   * <p><code>M/44'/coin type'/account'/key purpose/index</code></p>
   * <p>Notes:</p>
   * <ol>
   * <li>Coin type is 0' for Bitcoin</li>
   * <li>Account is 0-based and will be hardened when necessary (e.g. 0x80000000)</li>
   * <li>Key purpose resolves as 0 for external (receiving), 1 for internal (change) but other values may come later</li>
   * <li>Index is 0-based and identifies a particular address</li>
   * </ol>
   *
   * @param account     The plain account number (0 gives maximum compatibility)
   * @param keyPurpose  The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index       The plain index of the required address
   * @param showDisplay True if the device should display the same address to allow the user to verify no tampering has occurred (recommended).
   */
  public void requestAddress(int account, KeyChain.KeyPurpose keyPurpose, int index, boolean showDisplay) {

    // Set the FSM context
    context.beginGetAddressUseCase(account, keyPurpose, index, showDisplay);

  }

  /**
   * <p>Request a public key from the device. The device will respond by providing the public key calculated
   * based on the <a href="https://en.bitcoin.it/wiki/BIP_0044">BIP-44</a> deterministic wallet approach from
   * the master node.</p>
   *
   * <p>The BIP-44 chain code is arranged as follows:</p>
   * <p><code>M/44'/coin type'/account'/key purpose/index</code></p>
   * <p>Notes:</p>
   * <ol>
   * <li>Coin type is 0' for Bitcoin</li>
   * <li>Account is 0-based and will be hardened when necessary (e.g. 0x80000000)</li>
   * <li>Key purpose resolves as 0 for external (receiving), 1 for internal (change) but other values may come later</li>
   * <li>Index is 0-based and identifies a particular address</li>
   * </ol>
   *
   * @param account    The plain account number (0 gives maximum compatibility)
   * @param keyPurpose The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index      The plain index of the required address
   */
  public void requestPublicKey(int account, KeyChain.KeyPurpose keyPurpose, int index) {

    // Set the FSM context
    context.beginGetPublicKeyUseCase(account, keyPurpose, index);

  }

  /**
   * <p>Request a deterministic hierarchy based on the given child numbers.</p>
   *
   * <p>This can be used to create a "watching wallet" that does not contain any private keys so long
   * as all hardened child numbers are included.</p>
   *
   * @param childNumbers The list of child numbers representing a path that may include hardened entries
   */
  public void requestDeterministicHierarchy(List<ChildNumber> childNumbers) {

    // Set the FSM context
    context.beginGetDeterministicHierarchyUseCase(childNumbers);

  }

  /**
   * <p>Request some data to be encrypted or decrypted using an address key from the device. The device will respond by providing
   * the encrypted/decrypted data based on the key derived using the <a href="https://en.bitcoin.it/wiki/BIP_0044">BIP-44</a> deterministic
   * wallet approach from the master node. <b>This data is unique to the seed phrase and is deterministic</b>.</p>
   *
   * <p>The BIP-44 chain code is arranged as follows:</p>
   * <p><code>M/44'/coin type'/account'/key purpose/index</code></p>
   * <p>Notes:</p>
   * <ol>
   * <li>Coin type is 0' for Bitcoin</li>
   * <li>Account is 0-based and will be hardened when necessary (e.g. 0x80000000)</li>
   * <li>Key purpose resolves as 0 for external (receiving), 1 for internal (change) but other values may come later</li>
   * <li>Index is 0-based and identifies a particular address</li>
   * </ol>
   *
   * @param account      The plain account number (0 gives maximum compatibility)
   * @param keyPurpose   The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index        The plain index of the required address
   * @param displayText  The cipher key shown to the user (e.g. "User message")
   * @param keyValue     The key value (e.g. "[16 bytes of random data]")
   * @param isEncrypting True if encrypting
   * @param askOnDecrypt True if device should ask on decrypting
   * @param askOnEncrypt True if device should ask on encrypting
   */
  public void requestCipherKey(
    int account,
    KeyChain.KeyPurpose keyPurpose,
    int index,
    byte[] displayText,
    byte[] keyValue,
    boolean isEncrypting,
    boolean askOnDecrypt,
    boolean askOnEncrypt
  ) {

    // Set the FSM context
    context.beginCipherKeyUseCase(
      account,
      keyPurpose,
      index,
      displayText,
      keyValue,
      isEncrypting,
      askOnDecrypt,
      askOnEncrypt
    );
  }

  /**
   * <p>Request some data to be signed using an address key from the device. The device will respond by providing
   * the signed data based on the key derived using the <a href="https://en.bitcoin.it/wiki/BIP_0044">BIP-44</a> deterministic
   * wallet approach from the master node.</p>
   *
   * <p>The BIP-44 chain code is arranged as follows:</p>
   * <p><code>M/44'/coin type'/account'/key purpose/index</code></p>
   * <p>Notes:</p>
   * <ol>
   * <li>Coin type is 0' for Bitcoin</li>
   * <li>Account is 0-based and will be hardened when necessary (e.g. 0x80000000)</li>
   * <li>Key purpose resolves as 0 for external (receiving), 1 for internal (change) but other values may come later</li>
   * <li>Index is 0-based and identifies a particular address</li>
   * </ol>
   *
   * @param account    The plain account number (0 gives maximum compatibility)
   * @param keyPurpose The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index      The plain index of the required address
   * @param message    The message for signing
   */
  public void signMessage(int account, KeyChain.KeyPurpose keyPurpose, int index, byte[] message) {

    // Set the FSM context
    context.beginSignMessageUseCase(
      account,
      keyPurpose,
      index,
      message
    );
  }

  /**
   * <p>Request that the device signs the given transaction (limited number of inputs/outputs).</p>
   *
   * @param transaction             The transaction containing all the inputs and outputs
   * @param receivingAddressPathMap The paths to the receiving addresses for this transaction keyed by input index
   * @param changeAddressPathMap    The paths to the change address for this transaction keyed by Address
   */
  public void simpleSignTx(
    Transaction transaction,
    Map<Integer, ImmutableList<ChildNumber>> receivingAddressPathMap,
    Map<Address, ImmutableList<ChildNumber>> changeAddressPathMap) {

    throw new UnsupportedOperationException("Not yet supported. Use signTx instead.");

  }

  /**
   * <p>Request that the device signs the given transaction (unlimited number of inputs/outputs).</p>
   *
   * @param transaction             The transaction containing all the inputs and outputs
   * @param receivingAddressPathMap The paths to the receiving addresses for this transaction keyed by input index
   * @param changeAddressPathMap    The paths to the change address for this transaction keyed by Address
   */
  public void signTx(Transaction transaction, Map<Integer, ImmutableList<ChildNumber>> receivingAddressPathMap, Map<Address, ImmutableList<ChildNumber>> changeAddressPathMap) {

    // Set the FSM context
    context.beginSignTxUseCase(transaction, receivingAddressPathMap, changeAddressPathMap);

  }

  /**
   * <p>Request that the device encrypts the given message.</p>
   *
   * @param message The message for signing
   */
  public void encryptMessage(byte[] message) {

    throw new UnsupportedOperationException("Not yet supported. Please raise an issue and consider offering a bounty.");

  }

  /**
   * @return 32 bytes (256 bits) of entropy generated locally
   */
  public byte[] generateEntropy() {

    // Initialize a secure random number generator using
    // the OWASP recommended method
    SecureRandom secureRandom;
    try {
      secureRandom = SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e);
    }

    // Generate random bytes
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);

    return bytes;
  }
}
