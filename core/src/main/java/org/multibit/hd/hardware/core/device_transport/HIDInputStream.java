package org.multibit.hd.hardware.core.device_transport;

import com.codeminders.hidapi.HIDDevice;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>Input stream to provide the following to HID API:</p>
 * <ul>
 * <li>A buffered (max 63 bytes) input stream based on a blocking read of a single HID message</li>
 * <li>Removal of the HID-specific framing bytes</li>
 * </ul>
 * <p>It is intended that only a single input stream is associated with a single device</p>
 */
public class HIDInputStream extends InputStream {

  /**
   * Provides logging for this class
   */
  private static final Logger log = LoggerFactory.getLogger(HIDInputStream.class);

  /**
   * The HID device
   */
  private final HIDDevice device;

  /**
   * The message buffer is a stacked set of HID payloads
   */
  private byte[] messageBuffer = new byte[64];

  /**
   * The frame index is the location within the frame buffer for the next read
   */
  private int messageIndex = 0;

  /**
   * @param device The HID device providing the low-level communications
   * @throws java.io.IOException If something goes wrong
   */
  public HIDInputStream(HIDDevice device) throws IOException {

    Preconditions.checkNotNull(device, "Device must be present");

    this.device = device;
  }

  @Override
  public synchronized int read() throws IOException {

    // Check if a HID read is required
    if (messageIndex == 0) {

      bufferAllFrames();

    }

    log.trace("Reading {} of {}",messageIndex, messageBuffer.length-1);

    if (messageBuffer.length==0) {
      log.debug("No data so return EOF");
      return -1;
    }

    // Must have data to be here

    // Convert from byte to unsigned int
    int frameByte = messageBuffer[messageIndex] & 0xFF;

    messageIndex++;

    if (messageIndex >= messageBuffer.length) {
      log.trace("Message buffer reset");
      messageIndex = 0;
      messageBuffer = new byte[64];
    }

    log.trace("Frame byte is {}",frameByte);
    return frameByte;

  }

  @Override
  public void close() throws IOException {
    super.close();

    device.close();

  }

  /**
   * <p>Wrap the device read method to allow for easier unit testing (Mockito cannot handle native methods)</p>
   * <p>An optional timeout is provided to allow multi-frame messages to be detected. Usually this should be about
   * 50ms after an initial blocking call triggered by an absent timeout.</p>
   *
   * @param hidBuffer    The buffer contents to accept bytes from the device
   * @param durationMillis The milliseconds to wait before giving up (absent means blocking)
   * @return The number of bytes read (zero on a timeout)
   * @throws java.io.IOException If something goes wrong
   */
  /* package */ int readFromDevice(byte[] hidBuffer, Optional<Integer> durationMillis) throws IOException {

    if (durationMillis.isPresent()) {
      return device.readTimeout(hidBuffer, durationMillis.get());
    } else {
      return device.read(hidBuffer);
    }
  }

  /**
   * <p>Handles the process of reading in all the HID frames and extracting the payload from each into a single
   * message buffer. If a timeout occurs during the read operation then the message buffer is deemed to have been
   * fully populated.</p>
   *
   * @throws java.io.IOException If something goes wrong
   */
  private void bufferAllFrames() throws IOException {

    log.debug("Buffering all HID frames from device");

    // The insert position for any new HID payload
    int messageBufferFrameIndex = 0;

    boolean finished = false;
    while (!finished) {
      // Create a fresh HID message buffer
      byte[] hidBuffer = new byte[64];

      // Attempt to read the next 64-byte message (timeout on fail)
      int bytesRead;
      if (messageBufferFrameIndex==0) {

        log.debug("Blocking read");

        // First read is blocking (we want to hold here)
        bytesRead = readFromDevice(hidBuffer, Optional.<Integer>absent());
      } else {
        // Subsequent reads are to cover multiple frames building to an overall message
        bytesRead = readFromDevice(hidBuffer, Optional.of(500));
      }

      if (bytesRead > 0) {

        log.debug("< {} '{}'", bytesRead, hidBuffer);

        // Check for data error
        int frameLength = hidBuffer[0];

        if (frameLength > 63) {
          throw new IOException("Frame length cannot be > 63: " + frameLength);
        }

        // Check for a message buffer resize
        if (messageBufferFrameIndex + frameLength > messageBuffer.length) {
          messageBuffer = fitToLength(messageBuffer, messageBufferFrameIndex + frameLength + 64);
        }

        // Copy from the HID buffer into the overall message buffer
        // ignoring the first byte since it is for HID only
        System.arraycopy(hidBuffer, 1, messageBuffer, messageBufferFrameIndex, frameLength);

        // Keep track of the next insertion position
        messageBufferFrameIndex += frameLength;

      } else {
        log.debug("HID timeout - all data received.");
        finished = true;
      }
    }

    // Truncate the message buffer to the exact required size
    messageBuffer = fitToLength(messageBuffer, messageBufferFrameIndex);

  }

  private byte[] fitToLength(byte[] oldBuffer, int newLength) {

    byte[] newBuffer = new byte[newLength];

    System.arraycopy(oldBuffer, 0, newBuffer, 0, newLength > oldBuffer.length ? oldBuffer.length : newLength);

    log.debug("Re-sized message buffer: {} '{}'", newBuffer.length, newBuffer);

    return newBuffer;

  }
}
