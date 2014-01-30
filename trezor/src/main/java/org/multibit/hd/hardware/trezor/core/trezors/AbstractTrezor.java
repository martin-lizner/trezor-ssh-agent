package org.multibit.hd.hardware.trezor.core.trezors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.trezor.core.Trezor;
import org.multibit.hd.hardware.trezor.core.TrezorEvent;
import org.multibit.hd.hardware.trezor.core.TrezorEventType;
import org.multibit.hd.hardware.trezor.core.TrezorListener;
import org.multibit.hd.hardware.trezor.core.events.TrezorEvents;
import org.multibit.hd.hardware.trezor.core.protobuf.MessageType;
import org.multibit.hd.hardware.trezor.core.protobuf.TrezorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>Abstract base class to provide the following to Trezor devices:</p>
 * <ul>
 * <li>Access to common methods</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public abstract class AbstractTrezor implements Trezor {

  private static final Logger log = LoggerFactory.getLogger(AbstractTrezor.class);

  public static final int MAX_QUEUE_SIZE = 32;

  protected final Set<TrezorListener> listeners = Sets.newLinkedHashSet();

  // Provide a few threads for monitoring for specialised cases
  protected final ExecutorService trezorMonitorService = Executors.newFixedThreadPool(5);

  @Override
  public synchronized void addListener(TrezorListener trezorListener) {

    Preconditions.checkState(listeners.add(trezorListener), "Listener is already present");

    // Create a new queue for events
    BlockingQueue<TrezorEvent> listenerQueue = Queues.newArrayBlockingQueue(MAX_QUEUE_SIZE);
    trezorListener.setTrezorEventQueue(listenerQueue);
  }

  @Override
  public synchronized void removeListener(TrezorListener trezorListener) {

    Preconditions.checkState(listeners.remove(trezorListener), "Listener was not present");

    // Remove the queue
    trezorListener.setTrezorEventQueue(null);
  }

  /**
   * <p>Create an executor service to monitor the data input stream and raise events</p>
   */
  protected void monitorDataInputStream(final DataInputStream in) {

    // Monitor the data input stream
    trezorMonitorService.submit(new Runnable() {
      @Override
      public void run() {

        while (true) {
          try {
            // Read a message (blocking)
            final TrezorEvent trezorEvent = readMessage(in);

            emitTrezorEvent(trezorEvent);

            if (TrezorEventType.DEVICE_DISCONNECTED.equals(trezorEvent.eventType())) {
              // A shutdown is imminent so best to sleep to void multiple messages
              Thread.sleep(2000);
            } else {
              // Provide a small break
              Thread.sleep(100);
            }

          } catch (InterruptedException e) {
            break;
          }
        }

      }
    });

  }

  /**
   * <p>Broadcast a Trezor event to all the listeners</p>
   *
   * @param trezorEvent The event to fire
   *
   * @throws InterruptedException If interrupted
   */
  protected synchronized void emitTrezorEvent(TrezorEvent trezorEvent) throws InterruptedException {
    log.debug("Firing event: {} ", trezorEvent.eventType().name());
    for (TrezorListener listener : listeners) {
      listener.getTrezorEventQueue().put(trezorEvent);
    }
  }

  /**
   * <p>Blocking method to read from the data input stream</p>
   *
   * @param in The data input stream (must be open)
   *
   * @return The expected protocol buffer message for the detail
   */
  private synchronized TrezorEvent readMessage(DataInputStream in) {

    // Very broad try-catch because a lot of things can go wrong here and need to be reported
    try {

      // Read and throw away the magic header markers
      in.readByte();
      in.readByte();

      // Read the header code and select a suitable parser
      final Short headerCode = in.readShort();
      final MessageType messageType = MessageType.getMessageTypeByHeaderCode(headerCode);

      // Read the detail length
      final int detailLength = in.readInt();

      // Read the remaining bytes
      final byte[] detail = new byte[detailLength];
      final int actualLength = in.read(detail, 0, detailLength);

      // Verify the read
      Preconditions.checkState(actualLength == detailLength, "Detail not read fully. Expected=" + detailLength + " Actual=" + actualLength);

      // Parse the detail into a message
      final Message message = MessageType.parse(headerCode, detail);
      log.debug("< {}", message.getClass().getName());

      if (MessageType.FAILURE.equals(messageType)) {
        log.error("FAILED: {}", ((TrezorMessage.Failure) message).getMessage());
      }

      // Build the event from the given information
      return TrezorEvents.newProtocolEvent(messageType, message);

    } catch (EOFException e) {
      // Device has reached an unexpected EOF
      log.warn("Unexpected EOF from device");
      return TrezorEvents.newSystemEvent(TrezorEventType.DEVICE_EOF);
    } catch (IOException e) {
      // Device has likely disconnected during I/O
      log.warn("Unexpected disconnect from device.");
      return TrezorEvents.newSystemEvent(TrezorEventType.DEVICE_DISCONNECTED);
    } catch (Throwable e) {
      // System error
      log.error("Unexpected error during read.", e);
      return TrezorEvents.newSystemEvent(TrezorEventType.DEVICE_FAILURE);
    }

  }

  @Override
  public synchronized void close() {

    internalClose();
    trezorMonitorService.shutdownNow();

  }

  /**
   * <p>Implementations should handle their own shutdown before their threads are terminated</p>
   */
  public abstract void internalClose();

}
