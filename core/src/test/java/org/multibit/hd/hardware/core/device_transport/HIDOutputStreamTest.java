package org.multibit.hd.hardware.core.device_transport;

import com.codeminders.hidapi.HIDDevice;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;

public class HIDOutputStreamTest {

  // Note that Mockito cannot mock native methods
  // This is just to avoid a null reference and remove USB dependency from build environments
  private HIDDevice device = mock(HIDDevice.class);

  @Test
  public void verifySingleFrameUnbuffered() throws IOException {

    final byte[] expected = new byte[]{1, 2};

    HIDOutputStream testObject = HIDStreams.newSingleFrameHIDOutputStream(device, expected);

    testObject.write(new byte[]{2}, 0, 1);

    testObject.close();

  }

  @Test
  public void verifySingleFrameBuffered() throws IOException {

    final byte[] expected = HIDStreams.newHIDFrame(4);

    HIDOutputStream testObject = HIDStreams.newSingleFrameHIDOutputStream(device, expected);

    // Create the payload ignoring the initial length byte
    for (int i = 1; i < expected.length; i++) {
      testObject.write(expected[i]);
    }

    // This should perform the write with the length byte calculated
    testObject.flush();

    testObject.close();

  }

  @Test
  public void verifyMultiFrameUnbuffered() throws IOException {

    // Create a multi-frame HID
    final byte[][] expected = new byte[][]{
      HIDStreams.newHIDFrame(63),
      HIDStreams.newHIDFrame(3)
    };

    HIDOutputStream testObject = HIDStreams.newMultiFrameHIDOutputStream(device, expected);

    byte[] payload = new byte[66];
    System.arraycopy(HIDStreams.newHIDPayload(63), 0, payload, 0, 63);
    System.arraycopy(HIDStreams.newHIDPayload(3), 0, payload, 63, 3);

    testObject.write(payload, 0, payload.length);

    testObject.close();

  }

  @Test
  public void verifyMultiFrameBuffered() throws IOException {

    // Create a multi-frame HID
    final byte[][] expected = new byte[][]{
      HIDStreams.newHIDFrame(63),
      HIDStreams.newHIDFrame(3)
    };

    HIDOutputStream testObject = HIDStreams.newMultiFrameHIDOutputStream(device, expected);

    byte[] payload = new byte[66];
    System.arraycopy(HIDStreams.newHIDPayload(63), 0, payload, 0, 63);
    System.arraycopy(HIDStreams.newHIDPayload(3), 0, payload, 63, 3);

    for (byte b : payload) {
      testObject.write(b);
    }

    testObject.flush();

    testObject.close();

  }

}
