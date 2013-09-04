package uk.co.bsol.trezorj.core.usb;

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
    device.enableBlocking();
  }

  /**
   * <p>Write the byte[] to the device, spanning several USB frames if required</p>
   * <p>The message does not need to include HID information like message length etc</p>
   * <p>Bytes will be sent to the device immediately (no flush required) in frames of 63 bytes or less depending on
   * the size of the message.</p>
   * <p>This method is intended for fully-formed messages. To build up a message use a combination of {@link
   * #write(int)} and {@link #flush()} as would be the case with a <code>DataOutputStream</code> wrapper.</p>
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

      int frameBytesSent = writeToDevice(hidBuffer);
      if (frameBytesSent != hidBuffer.length) {
        throw new IOException("Unable to send bytes to device. Expected: " + hidBuffer.length + " Actual: " + frameBytesSent);
      }

      // Keep track of the overall number of bytes sent
      bytesSent += frameBytesSent;


      // Keep track of how many bytes are left to send
      leftToSend -= (frameBytesSent - 1);

      // Offset only applies to the first frame
      frameOffset = 0;

      log.info("> {} '{}' ", bytesSent, hidBuffer);
    }

  }

  /**
   * <p>Use this to build up a buffered message byte by byte (e.g. from a <code>DataOutputStream</code>).</p>
   * <p>If you have a complete message ready to go for direct write to the device then use
   * {@link HIDOutputStream#write(byte[], int, int)} instead </p>
   *
   * @param b The byte to send (downcast from int)
   * @throws IOException
   */
  @Override
  public void write(int b) throws IOException {
    baos.write(b);
  }

  @Override
  public void flush() throws IOException {
    write(baos.toByteArray(), 0, baos.size());
    baos.reset();
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
   * @throws IOException
   */
  /* package */ int writeToDevice(byte[] hidBuffer) throws IOException {
    return device.write(hidBuffer);
  }
}
