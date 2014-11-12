package org.multibit.hd.hardware.examples.trezor.wallet;

import org.bitcoinj.core.*;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class illustrating syncing of a bitcoinj wallet
 */

public class SyncWallet {

  private static final Logger log = LoggerFactory.getLogger(SyncWallet.class);

  // The address for this private key is "1GqtGtn4fctXuKxsVzRPSLmYWN1YioLi9y".
  private static final String MINING_PRIVATE_KEY = "5JDxPrBRghF1EvSBjDigywqfmAjpHPmTJxYtQTYJxJRHLLQA4mG";

  private static final String START_OF_REPLAY_PERIOD = "2012-03-03 13:00:00";
  private static Date replayDate;

  private static SimpleDateFormat formatter;

  private static PeerGroup peerGroup;

  private static String blockchainFilename;

  private static BlockChain blockChain;

  private static BlockStore blockStore;

  private static String checkpointsFilename;

  private static NetworkParameters networkParameters;

  public static final String SPV_BLOCKCHAIN_SUFFIX = ".spvchain";
  public static final String CHECKPOINTS_SUFFIX = ".checkpoints";

  static {
    try {
      java.text.SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      java.util.Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
      format.setCalendar(cal);
      replayDate = format.parse(START_OF_REPLAY_PERIOD);
    } catch (ParseException e) {
      handleError(e);
    }
  }

  public static void main(String[] args) throws Exception {

    // Date format is UTC with century, T time separator and Z for UTC timezone.
    formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

    networkParameters = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

    File multiBitDirectory = createMultiBitRuntime();

    // Create wallet
    String walletPath = multiBitDirectory.getAbsolutePath() + File.separator + "example.wallet";

    // Create a new wallet.
    Wallet exampleWallet = new Wallet(networkParameters);

    // Add in the mining key that has the coinbase transactions.
    DumpedPrivateKey miningPrivateKey = new DumpedPrivateKey(NetworkParameters.prodNet(), MINING_PRIVATE_KEY);

    exampleWallet.addKey(miningPrivateKey.getKey());

    exampleWallet.saveToFile(new File(walletPath));

    log.debug("Example wallet = \n" + exampleWallet.toString());

    try {
      // Load or create the blockStore..
      log.debug("Loading/ creating blockstore ...");
      blockStore = createBlockStore(replayDate);
      log.debug("Blockstore is '" + blockStore + "'");

      log.debug("Creating blockchain ...");
      blockChain = new BlockChain(networkParameters, blockStore);
      log.debug("Created blockchain '" + blockChain + "' with height " + blockChain.getBestChainHeight());

      log.debug("Creating peergroup ...");
      createNewPeerGroup(exampleWallet);
      log.debug("Created peergroup '" + peerGroup + "'");

      log.debug("Starting peergroup ...");
      peerGroup.start();
      log.debug("Started peergroup.");
    } catch (BlockStoreException e) {
      handleError(e);
    }

    //
    // MultiBit runtime is now setup and running.
    //

    // Wait for a peer connection.
    log.debug("Waiting for peer connection. . . ");
    while (peerGroup.getConnectedPeers().isEmpty()) {
      Thread.sleep(2000);
    }
    log.debug("Now online.");

    peerGroup.downloadBlockChain();

    // Tidy up.
    peerGroup.stop();
  }

  /**
   * Create a working, portable runtime of MultiBit in a temporary directory.
   *
   * @return the temporary directory the multibit runtime has been created in
   */
  private static File createMultiBitRuntime() throws IOException {
    File multiBitDirectory = createTempDirectory("multibit-hardware");
    String multiBitDirectoryPath = multiBitDirectory.getAbsolutePath();

    System.out.println("Building MultiBit runtime in : " + multiBitDirectory.getAbsolutePath());

    // Copy in the checkpoints stored in git - this is in src/main/resources/.
    File multibitCheckpoints = new File(multiBitDirectoryPath + File.separator + "multibit-hardware.checkpoints");

    File source = new java.io.File("./examples/src/main/resources/multibit-hardware.checkpoints");
    log.debug("Using source checkpoints file {}", source.getAbsolutePath());
    copyFile(source, multibitCheckpoints);
    multibitCheckpoints.deleteOnExit();
    log.debug("Copied checkpoints file to {}, size {} bytes", multibitCheckpoints.getAbsolutePath(), multibitCheckpoints.length());

    return multiBitDirectory;
  }

  private static File createTempDirectory(String filePrefix) throws IOException {
    final File temp;

    temp = File.createTempFile(filePrefix, Long.toString(System.currentTimeMillis()));

    if (!(temp.delete())) {
      throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
    }

    if (!(temp.mkdir())) {
      throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
    }

    temp.deleteOnExit();

    return temp;
  }

  private static void copyFile(File from, File to) throws IOException {

    if (!to.exists()) {
      to.createNewFile();
    }

    try (
            FileChannel in = new FileInputStream(from).getChannel();
            FileChannel out = new FileOutputStream(to).getChannel()) {

      out.transferFrom(in, 0, in.size());
    }
  }

  private static void handleError(Exception e) {
    log.error("Error creating SyncWallet " + e.getClass().getName() + " " + e.getMessage());
  }

  private static void createNewPeerGroup(Wallet wallet) {
    peerGroup = new PeerGroup(networkParameters, blockChain);
    peerGroup.setUserAgent("SyncWallet", "1");

    peerGroup.addPeerDiscovery(new DnsDiscovery(networkParameters));

    peerGroup.addWallet(wallet);

    // Add a PeerEventListener to the PeerGroup if you want to hear about blocks downloaded etc
  }

  private static BlockStore createBlockStore(Date checkpointDate) throws BlockStoreException, IOException {
    BlockStore blockStore = null;

    String filePrefix = "multibit-hardware";
    log.debug("filePrefix = " + filePrefix);

    blockchainFilename = filePrefix + SPV_BLOCKCHAIN_SUFFIX;
    checkpointsFilename = filePrefix + CHECKPOINTS_SUFFIX;


    File blockStoreFile = new File(blockchainFilename);
    boolean blockStoreCreatedNew = !blockStoreFile.exists();

    // Ensure there is a checkpoints file.
    File checkpointsFile = new File(checkpointsFilename);


    log.debug("Opening / Creating SPV block store '{}' from disk", blockchainFilename);
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
        if (checkpointDate == null) {
          if (blockStoreCreatedNew) {
            // Brand new block store - checkpoint from today. This
            // will go back to the last checkpoint.
            CheckpointManager.checkpoint(networkParameters, stream, blockStore, (new Date()).getTime() / 1000);
          }
        } else {
          // Use checkpoint date (block replay).
          CheckpointManager.checkpoint(networkParameters, stream, blockStore, checkpointDate.getTime() / 1000);
        }
      } finally {
        if (stream != null) {
          stream.close();
        }
      }
    }
    return blockStore;
  }
}

