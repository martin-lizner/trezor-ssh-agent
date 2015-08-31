package org.multibit.hd.hardware.examples.common.wallet;

import org.bitcoinj.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * <p>Implementation to provide the following to application:</p>
 * <ul>
 * <li>Logging of peer events</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class WatchingPeerEventListener implements PeerEventListener {

  private static final Logger log = LoggerFactory.getLogger(WatchingPeerEventListener.class);

  private Wallet wallet;

  public WatchingPeerEventListener(Wallet wallet) {
    this.wallet = wallet;
  }

  @Override
  public void onPeersDiscovered(Set<PeerAddress> peerAddresses) {

  }

  @Override
  public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
    if (blocksLeft % 100 == 0) {
      log.debug("Blocks left: {}", blocksLeft);
    }
  }

  @Override
  public void onChainDownloadStarted(Peer peer, int blocksLeft) {
    log.debug("Chain download stated: {}", blocksLeft);
  }

  @Override
  public void onPeerConnected(Peer peer, int peerCount) {

  }

  @Override
  public void onPeerDisconnected(Peer peer, int peerCount) {

  }

  @Override
  public Message onPreMessageReceived(Peer peer, Message m) {
    return null;
  }

  @Override
  public void onTransaction(Peer peer, Transaction transaction) {
    // Loop through all the wallets, seeing if the transaction is relevant and adding them as pending if so.
    if (transaction != null) {
      try {

        if (wallet.isTransactionRelevant(transaction)) {
          if (!(transaction.isTimeLocked()
                  && transaction.getConfidence().getSource() != TransactionConfidence.Source.SELF)
                  && wallet.isTransactionRisky(transaction, null)) {
            if (wallet.getTransaction(transaction.getHash()) == null) {
              log.debug("multibit-hardware adding a new pending transaction to the wallet: {}", transaction.toString());
              // The perWalletModelData is marked as dirty.

              wallet.receivePending(transaction, null);
            }
          }
        }
      } catch (VerificationException e) {
        e.printStackTrace();
      }
    }
  }

  @Nullable
  @Override
  public List<Message> getData(Peer peer, GetDataMessage m) {
    return null;
  }
}
