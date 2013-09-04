package uk.co.bsol.trezorj.core.usb;

import com.codeminders.hidapi.HIDDevice;
import org.junit.Test;

import java.io.IOException;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class HIDOutputStreamTest {

  // Note that Mockito cannot mock native methods
  // This is just to avoid a null reference and remove USB dependency from build environments
  private HIDDevice device = mock(HIDDevice.class);

  @Test
  public void verifySingleFrameUnbuffered() throws IOException {

    final byte[] expected = new byte[]{1, 2};

    HIDOutputStream testObject = newSingleFrameHIDOutputStream(expected);

    testObject.write(new byte[]{2}, 0, 1);

  }

  @Test
  public void verifySingleFrameBuffered() throws IOException {

    final byte[] expected = newHIDFrame(4);

    HIDOutputStream testObject = newSingleFrameHIDOutputStream(expected);

    // Create the payload ignoring the initial length byte
    for (int i = 1; i < expected.length; i++) {
      testObject.write(expected[i]);
    }

    // This should perform the write with the length byte calculated
    testObject.flush();

  }

  @Test
  public void verifyMultiFrameUnbuffered() throws IOException {

    // Create a multi-frame HID
    final byte[][] expected = new byte[][]{
      newHIDFrame(63),
      newHIDFrame(3)
    };

    HIDOutputStream testObject = newMultiFrameHIDOutputStream(expected);

    byte[] payload = new byte[66];
    System.arraycopy(newHIDPayload(63), 0, payload, 0, 63);
    System.arraycopy(newHIDPayload(3), 0, payload, 63, 3);

    testObject.write(payload, 0, payload.length);

  }

  @Test
  public void verifyMultiFrameBuffered() throws IOException {

    // Create a multi-frame HID
    final byte[][] expected = new byte[][]{
      newHIDFrame(63),
      newHIDFrame(3)
    };

    HIDOutputStream testObject = newMultiFrameHIDOutputStream(expected);

    byte[] payload = new byte[66];
    System.arraycopy(newHIDPayload(63), 0, payload, 0, 63);
    System.arraycopy(newHIDPayload(3), 0, payload, 63, 3);

    for (byte b : payload) {
      testObject.write(b);
    }

    testObject.flush();

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @param payloadLength The payload length (max 63 bytes)
   * @return A HID frame with first byte as the payload length (e.g. [3,0,1,2])
   */
  private byte[] newHIDFrame(int payloadLength) {

    byte[] hidPayload = newHIDPayload(payloadLength);
    byte[] hidFrame = new byte[payloadLength + 1];

    System.arraycopy(hidPayload, 0, hidFrame, 1, payloadLength);
    hidFrame[0] = (byte) payloadLength;

    return hidFrame;
  }

  /**
   * @param payloadLength The payload length (max 63 bytes)
   * @return A HID payload with no length byte (e.g. [0,1,2,3 ...])
   */
  private byte[] newHIDPayload(int payloadLength) {

    assertThat(payloadLength).isLessThan(64);
    assertThat(payloadLength).isGreaterThan(0);

    byte[] payload = new byte[payloadLength];

    for (int i = 0; i < payloadLength; i++) {
      payload[i] = (byte) (i);
    }

    return payload;
  }

  private HIDOutputStream newSingleFrameHIDOutputStream(final byte[] expected) throws IOException {

    return new HIDOutputStream(device) {

      // Wrap the native device method
      @Override
      int writeToDevice(byte[] hidBuffer) throws IOException {
        assertThat(hidBuffer).isEqualTo(expected);
        return expected.length;
      }
    };

  }

  private HIDOutputStream newMultiFrameHIDOutputStream(final byte[][] expected) throws IOException {

    return new HIDOutputStream(device) {

      int callCount = 0;

      // Wrap the native device method
      @Override
      int writeToDevice(byte[] hidBuffer) throws IOException {

        assertThat(hidBuffer).isEqualTo(expected[callCount]);
        int bytesSent = expected[callCount].length;
        callCount++;
        return bytesSent;
      }
    };

  }

}
