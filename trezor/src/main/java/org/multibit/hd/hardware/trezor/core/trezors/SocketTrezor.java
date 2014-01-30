package org.multibit.hd.hardware.trezor.core.trezors;

import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.trezor.core.Trezor;
import org.multibit.hd.hardware.trezor.core.TrezorEventType;
import org.multibit.hd.hardware.trezor.core.events.TrezorEvents;
import org.multibit.hd.hardware.trezor.core.utils.TrezorMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * <p>Trezor implementation to provide the following to applications:</p>
 * <ul>
 * <li>Access to a Trezor device over a socket</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public class SocketTrezor extends AbstractTrezor implements Trezor {

  private static final Logger log = LoggerFactory.getLogger(SocketTrezor.class);

  private Socket socket = null;
  private DataOutputStream out = null;

  private final String host;
  private final int port;

  /**
   * <p>Create a new socket connection to a Trezor device</p>
   *
   * @param host The host name or IP address (e.g. "192.168.0.1")
   * @param port The port (e.g. 3000)
   */
  public SocketTrezor(String host, int port) {

    Preconditions.checkNotNull(host, "'host' must be present");
    Preconditions.checkState(port > 0 && port < 65535, "'port' must be within range");

    this.host = host;
    this.port = port;

  }

  @Override
  public synchronized void connect() {

    Preconditions.checkState(socket == null, "Socket is already connected");

    try {

      // Attempt to open a socket to the host/port
      socket = new Socket(host, port);

      // Add buffered data streams for easy data manipulation
      out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 1024));
      DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 1024));

      // Monitor the input stream
      monitorDataInputStream(in);

      // Must have connected to be here
      emitTrezorEvent(TrezorEvents.newSystemEvent(TrezorEventType.DEVICE_CONNECTED));

    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public synchronized void internalClose() {

    Preconditions.checkNotNull(socket, "Socket is not connected. Use connect() first.");

    // Attempt to close the socket (also closes the in/out streams)
    try {
      socket.close();
      log.info("Disconnected from Trezor");
      // Let everyone know
      emitTrezorEvent(TrezorEvents.newSystemEvent(TrezorEventType.DEVICE_DISCONNECTED));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void sendMessage(Message message) {

    Preconditions.checkNotNull(message, "Message must be present");
    Preconditions.checkNotNull(out, "Socket has not been connected. Use connect() first.");

    try {
      // Apply the message to the data output stream
      TrezorMessageUtils.writeMessage(message, out);
    } catch (IOException e) {
      log.warn("I/O error during write. Closing socket.", e);
      try {
        emitTrezorEvent(TrezorEvents.newSystemEvent(TrezorEventType.DEVICE_DISCONNECTED));
      } catch (InterruptedException e1) {
        throw new IllegalStateException(e1);
      }
    }

  }
}
