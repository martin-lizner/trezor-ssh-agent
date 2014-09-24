package org.multibit.hd.hardware.core.device_transport;

import com.codeminders.hidapi.HIDDevice;
import org.junit.Test;

import java.io.IOException;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class HIDInputStreamTest {

  // Note that Mockito cannot mock native methods
  // This is just to avoid a null reference and remove USB dependency from build environments
  private HIDDevice device = mock(HIDDevice.class);

  @Test
  public void verifySingleFrameUnbuffered() throws IOException {

    final byte[] hidFrame = HIDStreams.newHIDFrame(2);

    byte[] expected = HIDStreams.newHIDPayload(2);
    byte[] actual = new byte[expected.length];

    HIDInputStream testObject = HIDStreams.newSingleFrameHIDInputStream(device, hidFrame);

    assertThat(testObject.read(actual)).isEqualTo(expected.length);
    assertThat(actual).isEqualTo(expected);

    testObject.close();

  }

    @Test
    public void verifySingleFrameBuffered() throws IOException {

      final byte[] hidFrame = HIDStreams.newHIDFrame(4);

      byte[] expected = HIDStreams.newHIDPayload(4);
      byte[] actual = new byte[expected.length];

      HIDInputStream testObject = HIDStreams.newSingleFrameHIDInputStream(device, hidFrame);

      // Create the payload through a series of single byte reads
      for (int i = 0; i < expected.length; i++) {
        byte b = (byte) testObject.read();
        actual[i] = b;
      }

      assertThat(actual).isEqualTo(expected);

      testObject.close();

    }

    @Test
    public void verifyMultiFrameUnbuffered() throws IOException {

      // Create a multi-frame HID
      final byte[][] hidFrames = new byte[][]{
        HIDStreams.newHIDFrame(63),
        HIDStreams.newHIDFrame(3)
      };

      byte[] expected = new byte[66];
      for (int i=0; i< expected.length; i++) {
        expected[i] = (byte) (i % 63);
      }
      byte[] actual = new byte[expected.length];

      HIDInputStream testObject = HIDStreams.newMultiFrameHIDInputStream(device, hidFrames);

      assertThat(testObject.read(actual, 0, expected.length)).isEqualTo(expected.length);

      assertThat(actual).isEqualTo(expected);

      testObject.close();

    }

    @Test
    public void verifyMultiFrameBuffered() throws IOException {

      // Create a multi-frame HID
      final byte[][] hidFrames = new byte[][]{
        HIDStreams.newHIDFrame(63),
        HIDStreams.newHIDFrame(3)
      };

      byte[] expected = new byte[66];
      for (int i=0; i< expected.length; i++) {
        expected[i] = (byte) (i % 63);
      }
      byte[] actual = new byte[expected.length];

      HIDInputStream testObject = HIDStreams.newMultiFrameHIDInputStream(device, hidFrames);

      for (int i = 0; i < expected.length; i++) {
        byte b = (byte) testObject.read();
        actual[i] = b;
      }

      assertThat(actual).isEqualTo(expected);

      testObject.close();

    }

}
