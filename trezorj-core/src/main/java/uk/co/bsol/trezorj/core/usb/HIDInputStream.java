package uk.co.bsol.trezorj.core.usb;

import com.codeminders.hidapi.HIDDevice;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
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

  private final HIDDevice device;

  /**
   * The message buffer is a stacked set of HID payloads
   */
  private byte[] messageBuffer = new byte[64];

  /**
   * The message buffer is a stacked set of HID payloads
   */
  private final ByteArrayInputStream bais = null;

  /**
   * The frame index is the location within the frame buffer for the next read
   */
  private int messageIndex = 0;

  /**
   * @param device The HID device providing the low-level communications
   *
   * @throws IOException If something goes wrong
   */
  public HIDInputStream(HIDDevice device) throws IOException {

    Preconditions.checkNotNull(device, "Device must be present");

    this.device = device;
  }

  @Override
  public int read() throws IOException {

    // Check if a read is required
    if (messageIndex == 0) {

      bufferAllFrames();

    }

    int frameByte = messageBuffer[messageIndex];

    messageIndex++;

    if (messageIndex >= messageBuffer.length) {
      log.debug("Message buffer reset");
      messageIndex = 0;
      messageBuffer = new byte[1024];
    }

    return frameByte;

  }

  /**
   * <p>Handles the process of reading in all the HID frames and extracting the payload from each into a single
   * message buffer. If a timeout occurs during the read operation then the message buffer is deemed to have been
   * fully populated.</p>
   *
   * @throws IOException If something goes wrong
   */
  private void bufferAllFrames() throws IOException {

    // The insert position for any new HID payload
    int messageBufferFrameIndex = 0;

    boolean finished = false;
    while (!finished) {
      // Create a fresh HID message buffer
      byte[] hidBuffer = new byte[64];

      // Attempt to read the next 64-byte message (timeout on fail)
      int bytesRead = readFromDevice(hidBuffer);

      if (bytesRead > 0) {

        log.debug("< {} '{}'", bytesRead, hidBuffer);

        // Check for data error
        int frameLength = hidBuffer[0];

        if (frameLength > 63) {
          throw new IOException("Frame length cannot be > 63: " + frameLength);
        }

        // Copy from the HID buffer into the overall message buffer
        // ignoring the first byte since it is for HID only
        System.arraycopy(hidBuffer, 1, messageBuffer, messageBufferFrameIndex, frameLength);

        // Keep track of the next insertion position
        messageBufferFrameIndex += frameLength;

        if (messageBufferFrameIndex > messageBuffer.length) {
          // Expand the message buffer
          messageBuffer = fitToLength(messageBuffer, messageBufferFrameIndex + 64);
        }

      } else {
        log.debug("HID timeout - all data received.");
        finished = true;
      }
    }

    // Truncate the message buffer to the exact required size
    messageBuffer = fitToLength(messageBuffer, messageBufferFrameIndex);

  }

  /**
   * <p>Wrap the device read method to allow for easier unit testing (Mockito cannot handle native methods)</p>
   *
   * @param hidBuffer The buffer contents to accept bytes from the device
   *
   * @return The number of bytes read
   *
   * @throws IOException If something goes wrong
   */
  /* package */ int readFromDevice(byte[] hidBuffer) throws IOException {
    return device.readTimeout(hidBuffer, 500);
  }

  @Override
  public void close() throws IOException {
    super.close();

    device.close();

  }

  private byte[] fitToLength(byte[] oldBuffer, int newLength) {

    byte[] newBuffer = new byte[newLength];

    System.arraycopy(oldBuffer, 0, newBuffer, 0, newLength > oldBuffer.length ? oldBuffer.length : newLength);

    log.debug("Resized message buffer: {} '{}'", newBuffer.length, newBuffer);

    return newBuffer;

  }
}
