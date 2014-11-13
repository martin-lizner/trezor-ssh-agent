package org.multibit.hd.hardware.examples.trezor.wallet;

import org.bitcoinj.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * <p>Implementation to provide the following to application:</p>
 * <ul>
 * <li>Logging of peer events</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class LoggingPeerEventListener implements PeerEventListener {

  private static final Logger log = LoggerFactory.getLogger(LoggingPeerEventListener.class);

  @Override
  public void onBlocksDownloaded(Peer peer, Block block, int blocksLeft) {
    log.debug("Blocks left: {}", blocksLeft);
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
  public void onTransaction(Peer peer, Transaction t) {

  }

  @Nullable
  @Override
  public List<Message> getData(Peer peer, GetDataMessage m) {
    return null;
  }
}
