package org.multibit.hd.hardware.core.device_transport;

import com.codeminders.hidapi.HIDDevice;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.Arrays;

/**
 * <p>Test utility to provide the following to HID tests:</p>
 * <ul>
 * <li>Various HID frame and payload methods</li>
 * </ul>
 */
public class HIDStreams {

  /**
   * @param payloadLength The payload length (max 63 bytes)
   *
   * @return A HID frame with first byte as the payload length (e.g. [3,0,1,2])
   */
  public static byte[] newHIDFrame(int payloadLength) {

    byte[] hidPayload = newHIDPayload(payloadLength);
    byte[] hidFrame = new byte[payloadLength + 1];

    System.arraycopy(hidPayload, 0, hidFrame, 1, payloadLength);
    hidFrame[0] = (byte) payloadLength;

    return hidFrame;
  }

  /**
   * @param payloadLength The payload length (max 63 bytes)
   *
   * @return A HID payload with no length byte (e.g. [0,1,2,3 ...])
   */
  public static byte[] newHIDPayload(int payloadLength) {

    Preconditions.checkState(payloadLength < 64, "'payloadLength' must be less than 64 bytes");
    Preconditions.checkState(payloadLength > 0, "'payloadLength' must be greater than 0 bytes");

    byte[] payload = new byte[payloadLength];

    for (int i = 0; i < payloadLength; i++) {
      payload[i] = (byte) (i);
    }

    return payload;
  }

  /**
   * @param device   The mock device to preserve behaviour
   * @param expected A HID frame (including message length byte) not exceeding 64 bytes
   *
   * @return A suitably configured HIDOutputStream
   *
   * @throws IOException If something goes wrong
   */
  public static HIDOutputStream newSingleFrameHIDOutputStream(HIDDevice device, final byte[] expected) throws IOException {

    return new HIDOutputStream(device) {

      // Wrap the native device method
      @Override
      int writeToDevice(byte[] hidBuffer) throws IOException {
        Preconditions.checkState(Arrays.equals(hidBuffer, expected), "'hidBuffer' is not the same as the expected frame");
        return expected.length;
      }
    };

  }

  /**
   * @param device   The mock device to preserve behaviour
   * @param expected The HID frames (including message length byte) not exceeding 64 bytes each to apply on subsequent calls
   *
   * @return A suitably configured HIDOutputStream
   *
   * @throws IOException If something goes wrong
   */
  public static HIDOutputStream newMultiFrameHIDOutputStream(HIDDevice device, final byte[][] expected) throws IOException {

    return new HIDOutputStream(device) {

      int callCount = 0;

      // Wrap the native device method
      @Override
      int writeToDevice(byte[] hidBuffer) throws IOException {

        Preconditions.checkState(Arrays.equals(hidBuffer, expected[callCount]),"");

        int bytesSent = expected[callCount].length;
        callCount++;
        return bytesSent;
      }
    };

  }

  /**
   * @param device   The mock device to preserve behaviour
   * @param hidFrame The HID frame (including message length byte) not exceeding 64 bytes total
   *
   * @return A suitably configured HIDInputStream
   *
   * @throws IOException If something goes wrong
   */
  public static HIDInputStream newSingleFrameHIDInputStream(HIDDevice device, final byte[] hidFrame) throws IOException {

    return new HIDInputStream(device) {

      int callCount = 0;

      // Wrap the native device method and perform an array copy
      @Override
      int readFromDevice(byte[] hidBuffer, Optional<Integer> durationMillis) throws IOException {

        if (callCount > 0) {
          return 0;
        }

        callCount++;

        System.arraycopy(hidFrame, 0, hidBuffer, 0, hidFrame.length);

        return hidFrame.length;
      }
    };

  }

  /**
   * @param device    The mock device to preserve behaviour
   * @param hidFrames The HID frames (including message length byte) not exceeding 64 bytes each to apply on subsequent calls
   *
   * @return A suitably configured HIDInputStream
   *
   * @throws IOException If something goes wrong
   */
  public static HIDInputStream newMultiFrameHIDInputStream(HIDDevice device, final byte[][] hidFrames) throws IOException {

    return new HIDInputStream(device) {

      int callCount = 0;

      // Wrap the native device method and perform an array copy
      @Override
      int readFromDevice(byte[] hidBuffer, Optional<Integer> durationMillis) throws IOException {

        if (callCount >= hidFrames.length) {
          return 0;
        }

        System.arraycopy(hidFrames[callCount], 0, hidBuffer, 0, hidFrames[callCount].length);

        int bytesRead = hidFrames[callCount].length;

        callCount++;

        return bytesRead;
      }
    };

  }

}
