package org.multibit.hd.hardware.emulators.trezor;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import org.multibit.hd.hardware.trezor.TrezorMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

/**
 * <p>Trezor emulator to provide the following to applications:</p>
 * <ul>
 * <li>A programmable Trezor emulator to offer up a sequence of protocol buffer messages</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class TrezorEmulator {

  private static final Logger log = LoggerFactory.getLogger(TrezorEmulator.class);

  private final List<EmulatorMessage> messages = Lists.newArrayList();

  private final Optional<DataOutputStream> outputStreamOptional;

  // Not used yet as input stream is not listened to
  private final Optional<DataInputStream> inputStreamOptional;

  // True if the emulator has been fully configured
  private boolean isBuilt = false;

  /**
   * <p>Utility method to provide a common sequence</p>
   * This emulator transmits over a socket - port 3000
   *
   * @return A default Trezor emulator with simple timed responses
   */
  public static TrezorEmulator newDefaultTrezorEmulator() {

    TrezorEmulator emulator = new TrezorEmulator(Optional.<DataOutputStream>absent(), Optional.<DataInputStream>absent());
    addSuccessMessage(emulator, 1, TimeUnit.SECONDS);

    return emulator;

  }

  /**
   * <p>Utility method to provide a common sequence</p>
   * This emulator sends and receives over streams
   *
   * @param transmitStream The stream the emulator will transmit replies to
   * @param receiveStream  The stream the emulator will receive data on
   *
   * @return A default Trezor emulator with simple timed responses
   */
  public static TrezorEmulator newStreamingTrezorEmulator(OutputStream transmitStream, InputStream receiveStream) {

    Preconditions.checkNotNull(transmitStream, "'transmitStream' must be present");
    // TODO Re-instate this check
    // Preconditions.checkNotNull(receiveStream,"'receiveStream' must be present");

    TrezorEmulator emulator = new TrezorEmulator(
      Optional.of(new DataOutputStream(transmitStream)),
      Optional.<DataInputStream>absent()
    );
    addSuccessMessage(emulator, 1, TimeUnit.SECONDS);

    return emulator;

  }

  public static void addSuccessMessage(TrezorEmulator trezorEmulator, int duration, TimeUnit timeUnit) {
    trezorEmulator.addMessage(new EmulatorMessage(
      TrezorMessage.Success
        .newBuilder()
        .setMessage("Emulator is up")
        .build(),
      duration,
      timeUnit
    ));
  }

  /**
   * Use the utility constructors
   */
  private TrezorEmulator(Optional<DataOutputStream> outputStreamOptional, Optional<DataInputStream> inputStreamOptional) {
    this.outputStreamOptional = outputStreamOptional;
    this.inputStreamOptional = inputStreamOptional;
  }


  /**
   * <p>Add a new emulator message to the queue</p>
   *
   * @param emulatorMessage The emulator message to add
   */
  public void addMessage(EmulatorMessage emulatorMessage) {

    validateState();

    log.debug("Adding '{}'", emulatorMessage.getTrezorMessage());

    messages.add(emulatorMessage);

  }

  /**
   * <p>Start the emulation process</p>
   */
  public Future<Boolean> start() throws ExecutionException, InterruptedException {

    // Prevent further modifications
    isBuilt = true;

    log.debug("Starting emulator");

    // Arrange
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    return executorService.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() {

        try {

          final DataOutputStream out;

          if (!outputStreamOptional.isPresent()) {
            ServerSocket serverSocket = new ServerSocket(3000);

            log.debug("Accepting connections on a socket");

            // Block until a connection is attempted
            Socket socket = serverSocket.accept();

            log.debug("Connected. Starting message sequence.");

            out = new DataOutputStream(socket.getOutputStream());
          } else {
            log.debug("Transmitting over a data stream");
            out = outputStreamOptional.get();
          }

          // Send some data (a Trezor SUCCESS response)
          for (EmulatorMessage message : messages) {

            long millis = message.getTimeUnit().toMillis(message.getDuration());

            log.debug("Sleeping {} millis", millis);

            // Wait for the required period of time
            Thread.sleep(millis);

            log.debug("Emulating '{}'", message.getTrezorMessage());

            TrezorMessageUtils.writeMessage(message.getTrezorMessage(), out);

          }

          // A connection has been made
          return true;
        } catch (IOException e) {
          log.error(e.getMessage(), e);
          return true;
        } catch (InterruptedException e) {
          log.error(e.getMessage(), e);
          return true;
        }
      }
    });

  }

  private void validateState() {
    if (isBuilt) {
      throw new IllegalStateException("Emulator is already built");
    }
  }

  /**
   * Immutable object representing a single Trezor message (usually a timed response)
   */
  public static class EmulatorMessage {

    private final Message trezorMessage;
    private final int duration;
    private final TimeUnit timeUnit;

    /**
     * @param trezorMessage The Trezor protobuf message
     * @param duration      The duration to wait before triggering the message
     * @param timeUnit      The time unit
     */
    public EmulatorMessage(Message trezorMessage, int duration, TimeUnit timeUnit) {
      this.trezorMessage = trezorMessage;
      this.duration = duration;
      this.timeUnit = timeUnit;
    }

    private Message getTrezorMessage() {
      return trezorMessage;
    }

    private int getDuration() {
      return duration;
    }

    private TimeUnit getTimeUnit() {
      return timeUnit;
    }
  }
}
