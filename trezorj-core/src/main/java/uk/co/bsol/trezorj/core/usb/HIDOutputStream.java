package uk.co.bsol.trezorj.core.usb;

import com.codeminders.hidapi.HIDDevice;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   * @throws java.io.IOException If something goes wrong
   */
  public HIDOutputStream(HIDDevice device) throws IOException {

    Preconditions.checkNotNull(device, "Device must be present");

    this.device = device;
    device.enableBlocking();
  }

  /**
   * <p>Write the byte[] to the device, spanning several USB frames if required</p>
   * <p>The message does not need to include HID information like message length etc</p>
   *
   * @param b   The bytes to send to the receiving device
   * @param off An initial offset (usually zero in this implementation)
   * @param len The number of bytes to send (usually <code>b.length</code>)
   * @throws IOException If something goes wrong
   */
  @Override
  public void write(byte[] b, int off, int len) throws IOException {

    int leftToSend = len;

    int frameOffset = off;
    int bytesSent = 0;

    while (leftToSend > 0) {

      // A frame has a maximum 63 bytes for payload
      int hidBufferLength = leftToSend > 63 ? 63 : leftToSend;

      // Allow an extra byte for the HID message content length
      byte[] hidBuffer = new byte[hidBufferLength + 1];
      hidBuffer[0] = (byte) hidBufferLength;

      // Copy the relevant part of the overall message into a 64 byte (or less) chunk
      System.arraycopy(b, frameOffset, hidBuffer, 1, hidBufferLength);

      // Keep track of the overall number of bytes sent
      bytesSent += device.write(hidBuffer);

      log.info("> {} '{}' ", bytesSent, hidBuffer);

      // Keep track of how many bytes are left to send
      leftToSend -= bytesSent;

      // Offset only applies to the first frame
      frameOffset = 0;

    }

  }

  /**
   * <p>This is an inefficient mechanism for sending bytes to the device - use {@link HIDOutputStream#write(byte[],
   * int, int)} instead</p>
   *
   * @param b The byte to send (downcast from int)
   * @throws IOException
   */
  @Override
  public void write(int b) throws IOException {
    this.write(new byte[]{(byte) b}, 0, 1);
  }

  @Override
  public void close() throws IOException {
    super.close();

    device.close();

  }
}
