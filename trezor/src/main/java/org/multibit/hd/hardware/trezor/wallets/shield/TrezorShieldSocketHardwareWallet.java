package org.multibit.hd.hardware.trezor.wallets.shield;

import com.google.common.base.Preconditions;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.events.MessageEventType;
import org.multibit.hd.hardware.core.events.MessageEvents;
import org.multibit.hd.hardware.trezor.wallets.AbstractTrezorHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * <p>Trezor implementation to provide the following to applications:</p>
 * <ul>
 * <li>Access to a Trezor emulator (such as a Shield on a RPi or Linux VM) communicating over a socket</li>
 * </ul>
 *
 * <p>This class uses standard sockets but includes HID framing</p>
 *
 * <h3>The Trezor Shield is not a primary device any longer so this code is probably out of date</h3>
 *
 * @since 0.0.1
 *  
 */
public class TrezorShieldSocketHardwareWallet extends AbstractTrezorHardwareWallet {

  private static final Logger log = LoggerFactory.getLogger(TrezorShieldSocketHardwareWallet.class);

  private Socket socket = null;
  private DataOutputStream out = null;
  private DataInputStream in = null;

  private String host;
  private int port;

  /**
   * Default constructor for use with dynamic binding
   */
  public TrezorShieldSocketHardwareWallet() {
    this("localhost", 3000);
  }

  /**
   * <p>Create a new socket connection to a Trezor device</p>
   *
   * @param host The host name or IP address (e.g. "192.168.0.1")
   * @param port The port (e.g. 3000)
   */
  public TrezorShieldSocketHardwareWallet(String host, int port) {

    Preconditions.checkNotNull(host, "'host' must be present");
    Preconditions.checkState(port > 0 && port < 65535, "'port' must be within range");

    this.host = host;
    this.port = port;

    attach();

  }

  @Override
  public void applySpecification(HardwareWalletSpecification specification) {

    super.applySpecification(specification);

    this.host = specification.getHost();
    this.port = specification.getPort();

  }

  @Override
  public boolean attach() {

    // Socket library will work
    return true;

  }

  @Override
  public void softDetach() {

    if (socket == null) {
      return;
    }

    // Attempt to close the socket (also closes the in/out streams)
    try {
      socket.close();
      log.info("Detached from Trezor");

      // Must have disconnected to be here
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DETACHED);

    } catch (IOException e) {
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_FAILED);
    }
  }

  @Override
  public void hardDetach() {

    if (socket == null) {
      return;
    }

    // Attempt to close the socket (also closes the in/out streams)
    try {
      socket.close();
      log.info("Hard detach from Trezor");

      // Must have detached to be here
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DETACHED);

    } catch (IOException e) {
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_FAILED);
    }

  }

  @Override
  public synchronized boolean connect() {

    Preconditions.checkState(socket == null, "Socket is already connected");

    try {

      // Attempt to open a socket to the host/port
      socket = new Socket(host, port);

      // Add buffered data streams for easy data manipulation
      out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 1024));
      in = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 1024));

      // Monitor the input stream
      //monitorDataInputStream(in);

      // Must have connected to be here
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_CONNECTED);

      return true;

    } catch (IOException e) {
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_FAILED);
    }

    // Must have failed to be here
    return false;
  }

  @Override
  protected com.google.common.base.Optional<MessageEvent> readFromDevice(int duration, TimeUnit timeUnit) {

    // TODO Implement this using the Trezor V1 as a template

    return null;
  }

  @Override
  protected int writeToDevice(byte[] buffer) {

    Preconditions.checkNotNull(buffer, "'buffer' must be present");
    Preconditions.checkNotNull(out, "Socket has not been connected. Use connect() first.");

    try {
      // Apply the message to the data output stream
      out.write(buffer);
      return buffer.length;
    } catch (IOException e) {
      log.warn("I/O error during write. Closing socket.", e);

      // Must have disconnected to be here
      MessageEvents.fireMessageEvent(MessageEventType.DEVICE_DETACHED);
    }

    // Must have failed to be here
    return -1;
  }

}
