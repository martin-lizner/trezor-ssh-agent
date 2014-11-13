package org.multibit.hd.hardware.examples.trezor.wallet;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Uninterruptibles;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.concurrent.SafeExecutors;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>Create a Bitcoinj watching wallet based on a deterministic hierarchy provided by a Trezor</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the message sequence to get a Bitcoinj deterministic hierarchy
 * from a Trezor that has an active wallet to enable a "watching wallet" to be created.</p>
 *
 * <h3>Only perform this example on a Trezor that you are using for test and development!</h3>
 * <h3>Do not send funds to any addresses generated from this xpub unless you have a copy of the seed phrase written down!</h3>
 *
 * @since 0.0.1
 *  
 */
public class TrezorWatchingWallet {

  private static final Logger log = LoggerFactory.getLogger(TrezorWatchingWallet.class);

  private static final String START_OF_REPLAY_PERIOD = "2014-11-10 00:00:00";
  private static Date replayDate;

  private static PeerGroup peerGroup;

  private static BlockChain blockChain;

  private static NetworkParameters networkParameters;

  public static final String SPV_BLOCKCHAIN_SUFFIX = ".spvchain";
  public static final String CHECKPOINTS_SUFFIX = ".checkpoints";

  private HardwareWalletService hardwareWalletService;

  private ListeningExecutorService walletService = SafeExecutors.newSingleThreadExecutor("wallet-service");

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

    // All the work is done in the class
    TrezorWatchingWallet example = new TrezorWatchingWallet();

    example.executeExample();

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
    HardwareWalletService.hardwareWalletEventBus.register(this);

    hardwareWalletService.start();

    // Simulate the main thread continuing with other unrelated work
    // We don't terminate main since we're using safe executors
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.HOURS);

  }

  /**
   * <p>Downstream consumer applications should respond to hardware wallet events</p>
   *
   * @param event The hardware wallet event indicating a state change
   */
  @Subscribe
  public void onHardwareWalletEvent(HardwareWalletEvent event) {

    log.debug("Received hardware event: '{}'.{}", event.getEventType().name(), event.getMessage());

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
        walletService.submit(new Runnable() {
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
    String walletPath = walletEnvironmentDirectory.getAbsolutePath() + File.separator + "watching.wallet";

    final Wallet watchingWallet;

    try {

      // Derive the first key
      DeterministicKey key_0_0 = hierarchy.deriveChild(
        Lists.newArrayList(
          ChildNumber.ZERO
        ),
        true,
        true,
        ChildNumber.ZERO
      );
      DeterministicKey key_1_0 = hierarchy.deriveChild(
        Lists.newArrayList(
          ChildNumber.ONE
        ),
        true,
        true,
        ChildNumber.ZERO
      );

      // Calculate the pubkeys
      ECKey pubkey_0_0 = ECKey.fromPublicOnly(key_0_0.getPubKey());
      Address address_0_0 = new Address(networkParameters, pubkey_0_0.getPubKeyHash());
      log.debug("Derived 0_0 address '{}'", address_0_0.toString());

      ECKey pubkey_1_0 = ECKey.fromPublicOnly(key_1_0.getPubKey());
      Address address_1_0 = new Address(networkParameters, pubkey_1_0.getPubKeyHash());
      log.debug("Derived 1_0 address '{}'", address_1_0.toString());

      watchingWallet = new Wallet(networkParameters);
      watchingWallet.addWatchedAddress(address_0_0);
      watchingWallet.addWatchedAddress(address_1_0);

      watchingWallet.saveToFile(new File(walletPath));

      log.debug("Example wallet = \n{}", watchingWallet.toString());

      // Load or create the blockStore..
      log.debug("Loading/creating block store...");
      BlockStore blockStore = createBlockStore(replayDate);
      log.debug("Block store is '" + blockStore + "'");

      log.debug("Creating block chain...");
      blockChain = new BlockChain(networkParameters, blockStore);
      log.debug("Created block chain '" + blockChain + "' with height " + blockChain.getBestChainHeight());

      log.debug("Creating peer group...");
      createNewPeerGroup(watchingWallet);
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

    log.info("Balance: {}", watchingWallet.getBalance());

    // Tidy up
    peerGroup.stopAsync();
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

    peerGroup.addEventListener(new LoggingPeerEventListener());

    peerGroup.addWallet(wallet);

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

