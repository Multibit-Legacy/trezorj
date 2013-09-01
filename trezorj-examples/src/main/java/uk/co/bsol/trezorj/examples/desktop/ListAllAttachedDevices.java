package uk.co.bsol.trezorj.examples.desktop;

import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.bsol.trezorj.core.protobuf.MessageType;
import uk.co.bsol.trezorj.core.protobuf.TrezorMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>Demonstrates that Java HID API is working on a desktop</p>
 * <p>Just execute {@link ListAllAttachedDevices#main(String[])}</p>
 *
 * @since 0.0.1
 *         
 */
public class ListAllAttachedDevices {

  private static final Logger log = LoggerFactory.getLogger(ListAllAttachedDevices.class);

  /**
   * Entry point to the example
   *
   * @param args No arguments
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // Initialise the HID library
    if (!ClassPathLibraryLoader.loadNativeHIDLibrary()) {
      throw new IllegalStateException("Unable to load native USB library (Win, Mac or Linux)");
    }

    // Get the HID manager
    HIDManager hidManager = HIDManager.getInstance();

    // Attempt to list the attached devices
    HIDDeviceInfo[] infos = hidManager.listDevices();
    if (infos == null) {
      throw new IllegalStateException("Unable to access connected device list. Check security policy.");
    }

    for (HIDDeviceInfo info : infos) {
      log.info("Attached device info: " + info.toString());
    }

    // Trezor RPi Shield
    // vendor_id=0x10c4
    // product_id=0xea80
    // serial = 002AF313
    HIDDevice device = hidManager.openById(0x10c4, 0xea80, "002AF313");

    log.info(device.getManufacturerString());
    log.info(device.getProductString());
    log.info(device.getSerialNumberString());

    // Ensure the UART is active
    uartEnable(device, true);

    // Clear out any existing data from both Rx and Tx
    uartPurgeFIFOs(device, (byte) 0x03);

    // Attempt to send a message
    StringBuilder sb = new StringBuilder();

    int count = 0;

    while (true) {

      AbstractMessage trezorMessage = TrezorMessage.Initialize
        .newBuilder()
        .setSessionId(ByteString.copyFrom("12345".getBytes()))
        .build();

      ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
      DataOutputStream out = new DataOutputStream(baos);


      try {

        writeMessage(trezorMessage, out);
        byte[] protobufMessageBuffer = baos.toByteArray();

        log.info("Protobuf message = {} '{}'", protobufMessageBuffer.length, protobufMessageBuffer);

        log.info("Bytes sent = {}", uartWriteAll(device, protobufMessageBuffer));

        Thread.sleep(500);

        //uartStatus(device);

        //uartPurgeFIFOs(device, (byte) 1);

        //Thread.sleep(5000);

        if (count > 4) {
          byte[] received = uartReadAll(device);
          log.info("Bytes received = {} '{}'", received.length, received);
          sb.append(new String(received));
          log.info("Built Message = '{}'", sb.toString());

        }

        count++;

        uartStatus(device);


      } catch (InterruptedException e) {
        break;
      }


    }

    device.close();

    // Finished
    hidManager.release();

  }

  /**
   * <p>Convert the protocol buffer message into a series of chunks so that the UART can handle them</p>
   *
   * @param device          The USB device
   * @param protobufMessage The protocol buffer message to send (will be split into 64 byte chunks for the UART)
   *
   * @return The total number of bytes sent (should be greater than length of protobufMessage)
   *
   * @throws IOException If something goes wrong
   */
  private static int uartWriteAll(HIDDevice device, byte[] protobufMessage) throws IOException {

    int leftToSend = protobufMessage.length;
    int protobufOffset = 0;
    int bytesSent = 0;

    while (leftToSend > 0) {

      // A chunk has a maximum 63 bytes for payload
      int chunkLength = leftToSend > 63 ? 63 : leftToSend;

      // Allow an extra byte for the chunk content length
      byte[] hidChunk = new byte[chunkLength + 1];
      hidChunk[0] = (byte) chunkLength;

      // Copy the relevant part of the overall message into a 64 byte (or less) chunk
      System.arraycopy(protobufMessage, protobufOffset, hidChunk, 1, chunkLength);

      // Keep track of the overall number of bytes sent
      bytesSent += device.write(hidChunk);

      log.info("> {} '{}' ", bytesSent, hidChunk);

      // Keep track of how many bytes are left to send
      leftToSend -= bytesSent;

    }

    return bytesSent;
  }

  /**
   * <p>Convert the UART chunks into a single protocol buffer message</p>
   *
   * @param device The USB device
   *
   * @return The assembled chunks (without HID report IDs) suitable for parsing at a higher level
   *
   * @throws IOException If something goes wrong
   */
  private static byte[] uartReadAll(HIDDevice device) throws IOException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
    DataOutputStream out = new DataOutputStream(baos);

    byte[] hidChunk = new byte[64];

    int bytesRead;
    do {

      // Attempt to read the next 64-byte chunk (if present)
      bytesRead = device.readTimeout(hidChunk, 500);

      if (bytesRead > 0) {

        int chunkLength = hidChunk[0];

        if (chunkLength > 63) {
          throw new IllegalArgumentException("Chunk length cannot be > 63: " + chunkLength);
        }

        // Append the data to the overall output stream (don't know in advance how large it will be)
        for (int i = 1; i < chunkLength + 1; i++) {
          out.writeByte(hidChunk[i]);
        }
      }
    } while (bytesRead > 0);

    byte[] received = baos.toByteArray();

    log.info("< {} '{}'", received.length, received);

    return received;
  }

  /**
   * <p>Reset the UART (equivalent to unplug then replug without warning)
   * </p>
   *
   * @param device The USB device
   *
   * @return The number of bytes sent in the feature report
   *
   * @throws IOException If something goes wrong
   */
  private static int uartReset(HIDDevice device) throws IOException {

    byte[] featureReport;
    featureReport = new byte[]{0x040, 0x00};

    int bytesSent = device.sendFeatureReport(featureReport);
    log.info("> UART Reset: {} '{}'", bytesSent, featureReport);

    return bytesSent;
  }

  /**
   * <p>Enable the UART to form the serial link</p>
   *
   * @param device  The USB device
   * @param enabled True if the UART is to be enabled
   *
   * @return The number of bytes sent in the feature report
   *
   * @throws IOException If something goes wrong
   */
  private static int uartEnable(HIDDevice device, boolean enabled) throws IOException {

    byte[] featureReport;
    if (enabled) {
      featureReport = new byte[]{0x041, 0x01};
    } else {
      featureReport = new byte[]{0x041, 0x00};

    }
    int bytesSent = device.sendFeatureReport(featureReport);
    log.info("> UART Enable: {} '{}'", bytesSent, featureReport);

    return bytesSent;
  }

  /**
   * <p>Get the UART status</p>
   * <p>The feature report is as follows:</p>
   * <ul>
   * <li>[0] (byte) : The report type</li>
   * <li>[1,2] (unsigned int) : number of bytes in Tx FIFO</li>
   * <li>[3,4] (unsigned int) : number of bytes in Rx FIFO</li>
   * <li>[5] (byte) : 1 if a parity error is in place, 2 if an overrun run has occurred</li>
   * <li>[6] (byte) : 0 if line break is not active, 1 if it is</li>
   * </ul>
   * <p>Reading the error clears it</p>
   *
   * @param device The USB device
   *
   * @return The feature report
   *
   * @throws IOException If something goes wrong
   */
  private static byte[] uartStatus(HIDDevice device) throws IOException {

    byte[] featureReport = new byte[10];
    featureReport[0] = 0x42;
    int bytesSent = device.getFeatureReport(featureReport);
    log.info("< UART Status: {} '{}'", bytesSent, featureReport);

    return featureReport;
  }

  /**
   * <p>Purge the Rx/Tx FIFOs on the UART</p>
   *
   * @param device    The USB device
   * @param purgeType 1 to purge Tx buffer, 2 to purge Rx buffer, 3 to purge both
   *
   * @return The number of bytes sent in the feature report
   *
   * @throws IOException If something goes wrong
   */
  private static int uartPurgeFIFOs(HIDDevice device, byte purgeType) throws IOException {

    byte[] enableRxTx = new byte[]{0x043, purgeType};
    int bytesSent = device.sendFeatureReport(enableRxTx);
    log.info("> Purge RxTx: {} '{}'", bytesSent, enableRxTx);

    return bytesSent;
  }


  /**
   * <p>Write a Trezor message to the output stream (header and protocol buffer bytes)</p>
   *
   * @param message The protocol buffer message to read
   * @param out     The data output stream (must be open)
   *
   * @throws java.io.IOException If something goes wrong
   */
  public static int writeMessage(AbstractMessage message, DataOutputStream out) throws IOException {

    // Require the header code
    short headerCode = MessageType.getHeaderCode(message);

    // Provide some debugging
    MessageType messageType = MessageType.getMessageTypeByHeaderCode(headerCode);
    log.debug("> {}", messageType.name());

    // Write magic alignment string
    out.write("##".getBytes());

    // Write header following Python's ">HL" syntax
    // > = Big endian, std size and alignment
    // H = Unsigned short (2 bytes) for header code
    // L = Unsigned long (4 bytes) for message length

    // Message type
    out.writeShort(headerCode);

    // Message length
    int length = message.getSerializedSize();
    out.writeInt(length);

    // Write the detail portion as a protocol buffer message
    message.writeTo(out);

    // Flush
    out.flush();

    // Return the total number of bytes in this message (including headers)
    return length + 8;
  }

}