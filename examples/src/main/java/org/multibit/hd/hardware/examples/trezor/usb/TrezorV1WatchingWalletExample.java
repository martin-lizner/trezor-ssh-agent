package org.multibit.hd.hardware.examples.trezor.usb;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.*;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.KeyChainGroup;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.concurrent.SafeExecutors;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.examples.trezor.wallet.WatchingPeerEventListener;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>Step 5 - Sign a transaction</p>
 * <p>Create a Bitcoinj watching wallet based on a deterministic hierarchy provided by a Trezor</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the message sequence to get a Bitcoinj deterministic hierarchy
 * from a Trezor that has an active wallet to enable a "watching wallet" to be created.</p>
 * <p/>
 * <h3>Only perform this example on a Trezor that you are using for test and development!</h3>
 * <h3>Do not send funds to any addresses generated from this xpub unless you have a copy of the seed phrase written down!</h3>
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1WatchingWalletExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorV1WatchingWalletExample.class);

  private static final String START_OF_REPLAY_PERIOD = "2014-11-01 00:00:00";
  private static Date replayDate;

  private static PeerGroup peerGroup;

  private static BlockChain blockChain;

  private static NetworkParameters networkParameters;

  public static final String SPV_BLOCKCHAIN_SUFFIX = ".spvchain";
  public static final String CHECKPOINTS_SUFFIX = ".checkpoints";

  private HardwareWalletService hardwareWalletService;

  private ListeningExecutorService walletService = SafeExecutors.newSingleThreadExecutor("wallet-service");
  private Wallet trezorWatchingWallet;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    java.text.SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    java.util.Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
    format.setCalendar(cal);
    replayDate = format.parse(START_OF_REPLAY_PERIOD);
    log.debug("Replay for this watching wallet will be performed from {}", replayDate.toString());

    // All the work is done in the class
    TrezorV1WatchingWalletExample example = new TrezorV1WatchingWalletExample();

    example.executeExample();

    // Simulate the main thread continuing with other unrelated work
    // We don't terminate main since we're using safe executors
    Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MINUTES);

  }

  /**
   * Execute the example
   */
  public void executeExample() {

    // Use factory to statically bind the specific hardware wallet
    TrezorV1HidHardwareWallet wallet = HardwareWallets.newUsbInstance(
      TrezorV1HidHardwareWallet.class,
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );

    // Wrap the hardware wallet in a suitable client to simplify message API
    HardwareWalletClient client = new TrezorHardwareWalletClient(wallet);

    // Wrap the client in a service for high level API suitable for downstream applications
    hardwareWalletService = new HardwareWalletService(client);

    // Register for the high level hardware wallet events
    HardwareWalletEvents.subscribe(this);

    hardwareWalletService.start();

  }

  /**
   * <p>Downstream consumer applications should respond to hardware wallet events</p>
   *
   * @param event The hardware wallet event indicating a state change
   */
  @Subscribe
  public void onHardwareWalletEvent(HardwareWalletEvent event) {

    log.debug("Received hardware event: '{}'.{}", event.getEventType().name(), event.getMessage());

    // Allow user input
    Scanner keyboard;

    switch (event.getEventType()) {
      case SHOW_DEVICE_FAILED:
        // Treat as end of example
        System.exit(0);
        break;
      case SHOW_DEVICE_DETACHED:
        // Can simply wait for another device to be connected again
        break;
      case SHOW_DEVICE_READY:
        if (hardwareWalletService.isWalletPresent()) {

          log.debug("Wallet is present. Requesting an address...");

          // Request the extended public key for the given account
          hardwareWalletService.requestDeterministicHierarchy(
            Lists.newArrayList(
              new ChildNumber(44 | ChildNumber.HARDENED_BIT),
              ChildNumber.ZERO_HARDENED,
              ChildNumber.ZERO_HARDENED
            ));

        } else {
          log.info("You need to have created a wallet before running this example");
        }

        break;
      case DETERMINISTIC_HIERARCHY:

        // Exit quickly from the event thread
        ListenableFuture<?> future = walletService.submit(new Runnable() {
          @Override
          public void run() {

            // Parent key should be M/44'/0'/0'
            DeterministicKey parentKey = hardwareWalletService.getContext().getDeterministicKey().get();
            log.info("Parent key path: {}", parentKey.getPathAsString());

            // Verify the deterministic hierarchy can derive child keys
            // In this case 0/0 from a parent of M/44'/0'/0'
            final DeterministicHierarchy hierarchy = hardwareWalletService.getContext().getDeterministicHierarchy().get();

            // Create a watching wallet
            createWatchingWallet(hierarchy);
          }
        });
        Futures.addCallback(future, new FutureCallback<Object>() {
          @Override
          public void onSuccess(@Nullable Object result) {
            log.error("Created watching wallet");
          }

          @Override
          public void onFailure(Throwable t) {
            log.error("Failed", t);
          }
        });

        break;
      case SHOW_PIN_ENTRY:
        // Device requires the current PIN to proceed
        PinMatrixRequest request = (PinMatrixRequest) event.getMessage().get();
        keyboard = new Scanner(System.in);
        String pin;
        switch (request.getPinMatrixRequestType()) {
          case CURRENT:
            System.err.println(
              "Recall your PIN (e.g. '1').\n" +
                "Look at the device screen and type in the numerical position of each of the digits\n" +
                "with 1 being in the bottom left and 9 being in the top right (numeric keypad style) then press ENTER."
            );
            pin = keyboard.next();
            hardwareWalletService.providePIN(pin);
            break;
        }
        break;
      case SHOW_OPERATION_SUCCEEDED:

        // Successful signature

        // Trezor will provide a signed serialized transaction
        byte[] deviceTxPayload = hardwareWalletService.getContext().getSerializedTx().toByteArray();

        byte[] signature0 = hardwareWalletService.getContext().getSignatures().get(0);

        try {
          log.info("DeviceTx payload:\n{}", Utils.HEX.encode(deviceTxPayload));
          log.info("DeviceTx signature0:\n{}", Utils.HEX.encode(signature0));

          // Load deviceTx
          Transaction deviceTx = new Transaction(MainNetParams.get(), deviceTxPayload);
          log.info("deviceTx:\n{}", deviceTx.toString());

          keyboard = new Scanner(System.in);
          System.err.println("Do you want to use your peer group to broadcast this transaction? (Y/N) ? ");
          String choice = keyboard.next();
          if (choice.toLowerCase().startsWith("y")) {

            log.info("Broadcasting via peer group...");
            peerGroup.broadcastTransaction(deviceTx);

          } else {

            log.info("Use http://blockchain.info/pushtx to broadcast this transaction to the Bitcoin network");
            log.info("DeviceTx pushtx:\n{}", Utils.HEX.encode(deviceTx.bitcoinSerialize()));

          }

        } catch (Exception e) {
          log.error("DeviceTx FAILED.", e);
        }

        // Treat as end of example
        System.exit(0);
        break;

      case SHOW_OPERATION_FAILED:
        log.error(event.getMessage().toString());
        // Treat as end of example
        System.exit(-1);
        break;
      default:
        // Ignore
    }
  }

  /**
   * @param hierarchy The deterministic hierarchy that forms the first node in the watching wallet
   */
  private void createWatchingWallet(DeterministicHierarchy hierarchy) {

    // Date format is UTC with century, T time separator and Z for UTC timezone.
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

    networkParameters = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

    final File walletEnvironmentDirectory;
    try {
      walletEnvironmentDirectory = createWalletEnvironment();
    } catch (IOException e) {
      handleError(e);
      return;
    }

    // Create wallet
    File walletFile = new File(walletEnvironmentDirectory.getAbsolutePath() + File.separator + "watching.wallet");

    try {

      if (walletFile.exists()) {

        trezorWatchingWallet = Wallet.loadFromFile(walletFile);

      } else {

        DeterministicKey rootNodePubOnly = hierarchy.getRootKey().getPubOnly();

        // Get the root public key and share the reference with the wallet since it will update
        // the known key search space which will be handy later
        KeyChainGroup keyChainGroup = new KeyChainGroup(
          networkParameters,
          rootNodePubOnly,
          (long) (replayDate.getTime() * 0.001),
          rootNodePubOnly.getPath()
        );

        // Using this key chain group will result in a watching wallet
        trezorWatchingWallet = new Wallet(networkParameters, keyChainGroup);

        // Immediately save
        trezorWatchingWallet.saveToFile(walletFile);

      }

      log.debug("Example wallet = \n{}", trezorWatchingWallet.toString());

      // Load or create the blockStore..
      log.debug("Loading/creating block store...");
      BlockStore blockStore = createBlockStore(replayDate);
      log.debug("Block store is '" + blockStore + "'");

      log.debug("Creating block chain...");
      blockChain = new BlockChain(networkParameters, blockStore);
      log.debug("Created block chain '" + blockChain + "' with height " + blockChain.getBestChainHeight());
      blockChain.addWallet(trezorWatchingWallet);

      log.debug("Creating peer group...");
      createNewPeerGroup(trezorWatchingWallet);
      log.debug("Created peer group '" + peerGroup + "'");

      log.debug("Starting peer group...");
      peerGroup.startAsync();
      log.debug("Started peer group.");

    } catch (Exception e) {
      handleError(e);
      return;
    }

    // Wallet environment is now setup and running

    // Wait for a peer connection
    log.debug("Waiting for peer connection. . . ");
    while (peerGroup.getConnectedPeers().isEmpty()) {
      Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
    }
    log.debug("Now online. Downloading block chain...");

    peerGroup.downloadBlockChain();

    log.info("Wallet after sync: {}", trezorWatchingWallet.toString());

    try {
      trezorWatchingWallet.saveToFile(walletFile);
    } catch (IOException e) {
      handleError(e);
      return;
    }

    // Now that we're synchronized create a spendable transaction

    // Build a spend transaction to an external address (ideally under your control)
    final Address spendAddress;
    try {
      spendAddress = new Address(MainNetParams.get(), "1AhN6rPdrMuKBGFDKR1k9A8SCLYaNgXhty");
    } catch (AddressFormatException e) {
      log.error(e.getMessage(), e);
      System.exit(-1);
      return;
    }

    // Spend 1 milli to the spend address
    Wallet.SendRequest sendRequest = Wallet.SendRequest.to(spendAddress, Coin.valueOf(100_000));
    sendRequest.missingSigsMode = Wallet.MissingSigsMode.USE_OP_ZERO;
    try {
      trezorWatchingWallet.completeTx(sendRequest);
    } catch (InsufficientMoneyException e) {
      log.error("Insufficient funds. Require at least 100 000 satoshis");
      System.exit(-1);
      return;
    }
    final Transaction spendToChangeTx = sendRequest.tx;

    // Create a map of transaction inputs to addresses (we expect funds as a Tx with single input to 0/0/0)
    Map<Integer, ImmutableList<ChildNumber>> receivingAddressPathMap = buildReceivingAddressPathMap(spendToChangeTx);
    Map<Address, ImmutableList<ChildNumber>> changeAddressPathMap = buildChangeAddressPathMap(spendToChangeTx);
    hardwareWalletService.signTx(spendToChangeTx, receivingAddressPathMap, changeAddressPathMap);

  }

  /**
   * Create a basic wallet environment (block store and checkpoints) in a temporary directory.
   *
   * @return The temporary directory for the wallet environment
   */
  private File createWalletEnvironment() throws IOException {

    File walletDirectory = new File(".");
    String walletDirectoryPath = walletDirectory.getAbsolutePath();

    log.debug("Building Wallet in: '{}'", walletDirectory.getAbsolutePath());

    // Copy in the checkpoints stored in git - this is in src/main/resources
    File checkpoints = new File(walletDirectoryPath + File.separator + "multibit-hardware.checkpoints");

    File source = new java.io.File("./examples/src/main/resources/multibit-hardware.checkpoints");
    log.debug("Using source checkpoints file {}", source.getAbsolutePath());

    copyFile(source, checkpoints);

    checkpoints.deleteOnExit();

    log.debug("Copied checkpoints file to '{}', size {} bytes", checkpoints.getAbsolutePath(), checkpoints.length());

    return walletDirectory;
  }

  /**
   * @param wallet The wallet managing the peers
   */
  private void createNewPeerGroup(Wallet wallet) {

    peerGroup = new PeerGroup(networkParameters, blockChain);
    peerGroup.setUserAgent("TrezorWatchingWallet", "1");

    peerGroup.addPeerDiscovery(new DnsDiscovery(networkParameters));

    peerGroup.addEventListener(new WatchingPeerEventListener(wallet));

    peerGroup.addWallet(wallet);

    peerGroup.recalculateFastCatchupAndFilter(PeerGroup.FilterRecalculateMode.FORCE_SEND_FOR_REFRESH);

  }

  /**
   * @param replayDate The date of the checkpoint file
   *
   * @return The new block store for synchronization
   *
   * @throws BlockStoreException
   * @throws IOException
   */
  private BlockStore createBlockStore(Date replayDate) throws BlockStoreException, IOException {

    BlockStore blockStore = null;

    String filePrefix = "multibit-hardware";
    log.debug("filePrefix = " + filePrefix);

    String blockchainFilename = filePrefix + SPV_BLOCKCHAIN_SUFFIX;
    String checkpointsFilename = filePrefix + CHECKPOINTS_SUFFIX;

    File blockStoreFile = new File(blockchainFilename);
    boolean blockStoreCreatedNew = !blockStoreFile.exists();

    // Ensure there is a checkpoints file.
    File checkpointsFile = new File(checkpointsFilename);

    log.debug("{} SPV block store '{}' from disk", blockStoreCreatedNew ? "Creating" : "Opening", blockchainFilename);
    try {
      blockStore = new SPVBlockStore(networkParameters, blockStoreFile);
    } catch (BlockStoreException bse) {
      handleError(bse);
    }

    // Load the existing checkpoint file and checkpoint from today.
    if (blockStore != null && checkpointsFile.exists()) {
      FileInputStream stream = null;
      try {
        stream = new FileInputStream(checkpointsFile);
        if (replayDate == null) {
          if (blockStoreCreatedNew) {
            // Brand new block store - checkpoint from today. This
            // will go back to the last checkpoint.
            CheckpointManager.checkpoint(networkParameters, stream, blockStore, (new Date()).getTime() / 1000);
          }
        } else {
          // Use checkpoint date (block replay).
          CheckpointManager.checkpoint(networkParameters, stream, blockStore, replayDate.getTime() / 1000);
        }
      } finally {
        if (stream != null) {
          stream.close();
        }
      }
    }

    return blockStore;

  }

  /**
   * @param tx The proposed transaction
   *
   * @return The receiving address path map linking the tx input index to a deterministic path
   */
  private Map<Integer, ImmutableList<ChildNumber>> buildReceivingAddressPathMap(Transaction tx) {

    Map<Integer, ImmutableList<ChildNumber>> receivingAddressPathMap = Maps.newHashMap();

    // Examine the Tx inputs to determine receiving addresses in use
    for (int i = 0; i < tx.getInputs().size(); i++) {
      TransactionInput input = tx.getInput(i);

      // Unsigned input script arranged as OP_0, PUSHDATA(33)[public key]
      Script script = input.getScriptSig();
      byte[] data = script.getChunks().get(1).data;

      DeterministicKey keyFromPubKey = trezorWatchingWallet.getActiveKeychain().findKeyFromPubKey(data);
      Preconditions.checkNotNull(keyFromPubKey, "Could not find deterministic key from given pubkey. Input script index: " + i);

      receivingAddressPathMap.put(i, keyFromPubKey.getPath());

    }

    return receivingAddressPathMap;
  }

  /**
   * @param tx The proposed transaction
   *
   * @return The receiving address path map linking the tx input index to a deterministic path
   */
  private Map<Address, ImmutableList<ChildNumber>> buildChangeAddressPathMap(Transaction tx) {

    Map<Address, ImmutableList<ChildNumber>> changeAddressPathMap = Maps.newHashMap();

    DeterministicKeyChain activeKeyChain = trezorWatchingWallet.getActiveKeychain();

    for (int i = 0; i < tx.getOutputs().size(); i++) {

      TransactionOutput output = tx.getOutput(i);

      Optional<DeterministicKey> key = Optional.absent();
      Optional<Address> address = Optional.absent();

      // Analyse the output script
      Script script = output.getScriptPubKey();
      if (script.isSentToRawPubKey()) {

        // Use the raw public key
        byte[] pubkey = script.getPubKey();
        if (trezorWatchingWallet.isPubKeyMine(pubkey)) {
          key = Optional.fromNullable(activeKeyChain.findKeyFromPubKey(pubkey));
          ECKey ecKey = ECKey.fromPublicOnly(pubkey);
          address = Optional.fromNullable(ecKey.toAddress(MainNetParams.get()));
        }

      } else if (script.isPayToScriptHash() && trezorWatchingWallet.isPayToScriptHashMine(script.getPubKeyHash())) {

        // Extract the public key hash from the script
        byte[] pubkeyHash = script.getPubKeyHash();
        key = Optional.fromNullable(activeKeyChain.findKeyFromPubHash(pubkeyHash));
        address = Optional.fromNullable(new Address(MainNetParams.get(), pubkeyHash));
      } else {

        // Use the public key hash
        byte[] pubkeyHash = script.getPubKeyHash();
        if (trezorWatchingWallet.isPubKeyHashMine(pubkeyHash)) {
          key = Optional.fromNullable(activeKeyChain.findKeyFromPubHash(pubkeyHash));
          address = Optional.fromNullable(new Address(MainNetParams.get(), pubkeyHash));
        }
      }

     if (key.isPresent() && address.isPresent()) {

       // Found an address we own
       changeAddressPathMap.put(address.get(), key.get().getPath());
     }

    }

    return changeAddressPathMap;
  }

  private void copyFile(File from, File to) throws IOException {

    if (!to.exists()) {
      if (!to.createNewFile()) {
        throw new IOException("Could not create '" + to.getAbsolutePath() + "'");
      }
    }

    try (
      FileChannel in = new FileInputStream(from).getChannel();
      FileChannel out = new FileOutputStream(to).getChannel()) {

      out.transferFrom(in, 0, in.size());
    }

  }

  private void handleError(Exception e) {
    log.error("Error creating watching wallet: " + e.getClass().getName() + " " + e.getMessage());

    // Treat as end of example
    System.exit(-1);
  }
}

