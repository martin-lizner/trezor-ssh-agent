package org.multibit.hd.hardware.emulators.generic;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.emulators.generic.protobuf.GenericMessage;
import org.multibit.hd.hardware.trezor.TrezorMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

/**
 * <p>Generic hardware wallet emulator to provide the following to applications:</p>
 * <ul>
 * <li>A programmable hardware wallet emulator to offer up protocol messages in a defined order</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class GenericSequenceEmulator {

  private static final Logger log = LoggerFactory.getLogger(GenericSequenceEmulator.class);

  private final List<EmulatorProtocolMessage> messages = Lists.newArrayList();

  private final Optional<DataOutputStream> outputStreamOptional;

  // Not used yet as input stream is not listened to
  private final Optional<DataInputStream> inputStreamOptional;

  // True if the emulator has been fully configured
  private boolean isBuilt = false;

  /**
   * <p>Utility method to provide a sequence consisting of</p>
   * <ol>
   * <li>SUCCESS after 1 second</li>
   * </ol>
   *
   * <p>This emulator transmits over a socket - port 3000</p>
   *
   * @return A default generic emulator with simple timed responses
   */
  public static GenericSequenceEmulator newSingleSuccessEmulator() {

    GenericSequenceEmulator emulator = new GenericSequenceEmulator(Optional.<DataOutputStream>absent(), Optional.<DataInputStream>absent());
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
  public static GenericSequenceEmulator newStreamingEmulator(OutputStream transmitStream, InputStream receiveStream) {

    Preconditions.checkNotNull(transmitStream, "'transmitStream' must be present");
    // TODO Re-instate this check
    // Preconditions.checkNotNull(receiveStream,"'receiveStream' must be present");

    GenericSequenceEmulator emulator = new GenericSequenceEmulator(
      Optional.of(new DataOutputStream(transmitStream)),
      Optional.<DataInputStream>absent()
    );
    addSuccessMessage(emulator, 1, TimeUnit.SECONDS);

    return emulator;

  }

  public static void addSuccessMessage(GenericSequenceEmulator trezorEmulator, int duration, TimeUnit timeUnit) {

    trezorEmulator.addMessage(new EmulatorProtocolMessage(
      GenericMessage.Success
        .newBuilder()
        .setMessage("Hello")
        .build(),
      duration,
      timeUnit
    ));
  }

  /**
   * Use the utility constructors
   */
  private GenericSequenceEmulator(Optional<DataOutputStream> outputStreamOptional, Optional<DataInputStream> inputStreamOptional) {

    this.outputStreamOptional = outputStreamOptional;
    this.inputStreamOptional = inputStreamOptional;

  }

  /**
   * <p>Add a new emulator message to the queue</p>
   *
   * @param emulatorProtocolMessage The emulator message to add
   */
  public void addMessage(EmulatorProtocolMessage emulatorProtocolMessage) {

    validateState();

    log.debug("Adding '{}'", emulatorProtocolMessage.getProtocolMessage());

    messages.add(emulatorProtocolMessage);

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
            int port = 3000;
            ServerSocket serverSocket = new ServerSocket(port);

            log.debug("Accepting connections on a socket port {}", port);

            // Block until a connection is attempted
            Socket socket = serverSocket.accept();

            log.debug("Connected. Starting message sequence.");

            out = new DataOutputStream(socket.getOutputStream());
          } else {
            log.debug("Transmitting over a data stream");
            out = outputStreamOptional.get();
          }

          // Work through the message sequence
          for (EmulatorProtocolMessage message : messages) {

            long millis = message.getTimeUnit().toMillis(message.getDuration());

            log.debug("Sleeping {} millis", millis);

            // Wait for the required period of time
            Thread.sleep(millis);

            log.debug("Emulating '{}'", message.getProtocolMessage());

            TrezorMessageUtils.writeMessage(message.getProtocolMessage(), out);

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
   * Immutable object representing a single protocol message (usually a timed response)
   */
  public static class EmulatorProtocolMessage {

    private final Message protocolMessage;
    private final int duration;
    private final TimeUnit timeUnit;

    /**
     * @param protocolMessage The protocol message
     * @param duration        The duration to wait before triggering the message
     * @param timeUnit        The time unit
     */
    public EmulatorProtocolMessage(Message protocolMessage, int duration, TimeUnit timeUnit) {
      this.protocolMessage = protocolMessage;
      this.duration = duration;
      this.timeUnit = timeUnit;
    }

    private Message getProtocolMessage() {
      return protocolMessage;
    }

    private int getDuration() {
      return duration;
    }

    private TimeUnit getTimeUnit() {
      return timeUnit;
    }
  }
}
