package org.multibit.hd.hardware.core.usb;

import com.codeminders.hidapi.HIDDevice;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Input stream to provide the following to HID API:</p>
 * <ul>
 * <li>A buffered (max 63 bytes) input stream based on a blocking read of a single HID message</li>
 * <li>Removal of the HID-specific framing bytes</li>
 * </ul>
 * <p>It is intended that only a single input stream is associated with a single device</p>
 */
public class HIDOutputStream extends OutputStream {

  /**
   * Provides logging for this class
   */
  private static final Logger log = LoggerFactory.getLogger(HIDOutputStream.class);

  private final HIDDevice device;

  private ByteArrayOutputStream baos = new ByteArrayOutputStream();

  /**
   * @param device The HID device providing the low-level communications
   * @throws java.io.IOException If something goes wrong
   */
  public HIDOutputStream(HIDDevice device) throws IOException {

    Preconditions.checkNotNull(device, "Device must be present");

    this.device = device;
  }

  /**
   * <p>Use this to build up a buffered message byte by byte (e.g. from a <code>DataOutputStream</code>).</p>
   * <p>If you have a complete message ready to go for direct write to the device then use
   * {@link HIDOutputStream#write(byte[], int, int)} instead </p>
   *
   * @param b The byte to send (downcast from int)
   * @throws java.io.IOException
   */
  @Override
  public void write(int b) throws IOException {
    baos.write(b);
  }

  @Override
  public void flush() throws IOException {

    byte[] messageBuffer = baos.toByteArray();
    baos.reset();

    log.debug("> Message buffer: {} '{}'", messageBuffer.length, messageBuffer);

    int messageBufferFrameIndex = 0;

    while (messageBufferFrameIndex < messageBuffer.length) {

      // A frame has a maximum 63 bytes for payload
      int hidBufferLength = messageBuffer.length - messageBufferFrameIndex > 63 ? 63 : messageBuffer.length - messageBufferFrameIndex;

      // Allow an extra byte for the HID message content length
      byte[] hidBuffer = new byte[hidBufferLength + 1];
      hidBuffer[0] = (byte) hidBufferLength;

      // Copy the relevant part of the overall message into a 64 byte (or less) chunk
      System.arraycopy(messageBuffer, messageBufferFrameIndex, hidBuffer, 1, hidBufferLength);

      int hidBytesSent = writeToDevice(hidBuffer);
      if (hidBytesSent != hidBuffer.length) {
        throw new IOException("Unable to send bytes to device. Expected: " + hidBuffer.length + " Actual: " + hidBytesSent);
      }

      // Adjust the frame index by the number of bytes sent (less 1 for the length)
      messageBufferFrameIndex += (hidBytesSent - 1);

    }


  }

  @Override
  public void close() throws IOException {
    super.close();

    device.close();

  }

  /**
   * <p>Wrap the device write method to allow for easier unit testing (Mockito cannot handle native methods)</p>
   *
   * @param hidBuffer The buffer contents to write to the device
   * @return The number of bytes written
   * @throws java.io.IOException
   */
  /* package */ int writeToDevice(byte[] hidBuffer) throws IOException {

    log.debug("> {} '{}' ", hidBuffer.length, hidBuffer);
    return device.write(hidBuffer);

  }
}
