package uk.co.bsol.trezorj.core.usb;

import com.codeminders.hidapi.HIDDevice;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>USB HID wrapper to provide the following to USB handlers:</p>
 * <ul>
 * <li>Low level read/write operations with a CP211x USB to UART bridge</li>
 * <li>Simple stream-based communication via a {@link com.codeminders.hidapi.HIDDevice}</li>
 * </ul>
 * <p>The device </p>
 */
public class CP211xBridge {

  /**
   * Provides logging for this class
   */
  private static final Logger log = LoggerFactory.getLogger(CP211xBridge.class);

  private final HIDDevice device;
  private final HIDInputStream hidInputStream;
  private final HIDOutputStream hidOutputStream;

  /**
   * @param device The HID device providing the low-level communications
   * @throws IOException If something goes wrong
   */
  public CP211xBridge(HIDDevice device) throws IOException {
    this.device = device;
    this.hidInputStream = new HIDInputStream(device);
    this.hidOutputStream = new HIDOutputStream(device);
  }

  /**
   * <p>Provides an input stream consisting of incoming HID message payload data (no length byte)</p>
   * <p>Applications will use this to read data from the device, perhaps into a Protcol Buffer parser</p>
   *
   * @return The HID input stream
   * @throws IOException If something goes wrong
   */
  public HIDInputStream getInputStream() throws IOException {
    return this.hidInputStream;
  }

  /**
   * <p>Provides an output stream consisting of outgoing HID message payload data (no length byte)</p>
   * <p>Applications will use this to write data to the device, perhaps from a Protcol Buffer serializer</p>
   *
   * @return The HID input stream
   * @throws IOException If something goes wrong
   */
  public HIDOutputStream getOutputStream() throws IOException {
    return this.hidOutputStream;

  }

  /**
   * <p>Reset the UART (equivalent to unplug then replug without warning)
   * </p>
   *
   * @return The number of bytes sent in the feature report
   * @throws IOException If something goes wrong
   */
  public int reset() throws IOException {

    Preconditions.checkNotNull(device, "Device is not connected");

    byte[] featureReport;
    featureReport = new byte[]{0x040, 0x00};

    int bytesSent = device.sendFeatureReport(featureReport);
    log.info("> UART Reset: {} '{}'", bytesSent, featureReport);

    return bytesSent;
  }

  /**
   * <p>Enable the UART to form the serial link</p>
   *
   * @param enabled True if the UART is to be enabled
   * @return The number of bytes sent in the feature report
   * @throws IOException If something goes wrong
   */
  public int enable(boolean enabled) throws IOException {

    Preconditions.checkNotNull(device, "Device is not connected");

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
   * @return The feature report
   * @throws IOException If something goes wrong
   */
  public byte[] status() throws IOException {

    Preconditions.checkNotNull(device, "Device is not connected");

    byte[] featureReport = new byte[10];
    featureReport[0] = 0x42;
    int bytesReceived = device.getFeatureReport(featureReport);
    log.info("< UART Status: {} '{}'", bytesReceived, featureReport);

    return featureReport;
  }

  /**
   * <p>Purge the Rx/Tx FIFOs on the UART</p>
   *
   * @param purgeType 1 to purge Tx buffer, 2 to purge Rx buffer, 3 to purge both
   * @return The number of bytes sent in the feature report
   * @throws IOException If something goes wrong
   */
  public int purge(int purgeType) throws IOException {

    Preconditions.checkNotNull(device, "Device is not connected");

    byte[] enableRxTx = new byte[]{0x043, (byte) purgeType};
    int bytesSent = device.sendFeatureReport(enableRxTx);
    log.info("> Purge RxTx: {} '{}'", bytesSent, enableRxTx);

    return bytesSent;
  }

}
