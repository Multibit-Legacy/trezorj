package uk.co.bsol.trezorj.core.usb;

import com.codeminders.hidapi.HIDDevice;
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

  private final HIDDevice device;

  /**
   * The frame buffer is a single HID frame
   */
  private byte[] frameBuffer;

  /**
   * The frame index is the location within the frame buffer for the next read
   */
  private int frameIndex = 0;

  /**
   * @param device The HID device providing the low-level communications
   * @throws IOException If something goes wrong
   */
  public HIDInputStream(HIDDevice device) throws IOException {

    Preconditions.checkNotNull(device, "Device must be present");

    this.device = device;
    device.enableBlocking();
  }

  @Override
  public int read() throws IOException {

    // Check if a blocking read is required
    if (frameIndex == 0) {

      // Create a fresh frame buffer
      frameBuffer = new byte[63];

      // Create a fresh HID message buffer
      byte[] hidBuffer = new byte[64];

      // Attempt to read the next 64-byte message (blocking)
      int bytesRead = device.read(hidBuffer);

      if (bytesRead > 0) {

        log.debug("< {} '{}'", bytesRead, hidBuffer);

        // Check for data error
        int frameLength = hidBuffer[0];

        if (frameLength > 63) {
          throw new IOException("Frame length cannot be > 63: " + frameLength);
        }

        // Copy from the HID buffer into the frame buffer
        // ignoring the first byte since it is for HID only
        System.arraycopy(hidBuffer, 1, frameBuffer, 0, frameLength);

      }

    }

    int frameByte = frameBuffer[frameIndex];

    frameIndex++;

    if (frameIndex > 63) {
      frameIndex = 0;
    }

    return frameByte;

  }

  @Override
  public void close() throws IOException {
    super.close();

    device.close();

  }
}
