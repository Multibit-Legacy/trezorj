package uk.co.bsol.trezorj.core.trezors;

import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.bsol.trezorj.core.Trezor;
import uk.co.bsol.trezorj.core.usb.CP211xBridge;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>Trezor implementation to provide the following to applications:</p>
 * <ul>
 * <li>Access to a Trezor device over USB</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public class UsbTrezor extends AbstractTrezor implements Trezor {

  private static final Integer DEFAULT_USB_VENDOR_ID = 0x10c4;
  private static final Integer DEFAULT_USB_PRODUCT_ID = 0xea80;

  private static final Logger log = LoggerFactory.getLogger(UsbTrezor.class);

  private Optional<Integer> vendorIdOptional = Optional.absent();
  private Optional<Integer> productIdOptional = Optional.absent();
  private Optional<String> serialNumberOptional = Optional.absent();

  private DataOutputStream out = null;

  private HIDDevice device = null;

  /**
   * <p>Create a new instance of a USB-based Trezor device (standard)</p>
   *
   * @param vendorIdOptional     The vendor ID (default is 0x10c4)
   * @param productIdOptional    The product ID (default is 0xea80)
   * @param serialNumberOptional The device serial number (default is to accept any)
   */
  public UsbTrezor(Optional<Integer> vendorIdOptional,
                   Optional<Integer> productIdOptional,
                   Optional<String> serialNumberOptional) {

    // Initialise the HID library
    if (!ClassPathLibraryLoader.loadNativeHIDLibrary()) {
      throw new IllegalStateException(
        "Unable to load native USB library. Check class loader permissions/JAR integrity.");
    }

    this.vendorIdOptional = vendorIdOptional;
    this.productIdOptional = productIdOptional;
    this.serialNumberOptional = serialNumberOptional;

  }

  @Override
  public synchronized void connect() {

    Preconditions.checkState(device == null, "Device is already connected");

    try {

      // Attempt to locate an attached Trezor device
      final Optional<HIDDeviceInfo> hidDeviceInfoOptional = locateTrezor();

      if (!hidDeviceInfoOptional.isPresent()) {
        throw new IllegalArgumentException("No matching Trezor device is connected.");
      }
      HIDDeviceInfo hidDeviceInfo = hidDeviceInfoOptional.get();

      // Get the HID manager
      HIDManager hidManager = HIDManager.getInstance();

      // Attempt to open a serial connection to the USB device
      // Open the device
      device = hidManager.openById(
        hidDeviceInfo.getVendor_id(),
        hidDeviceInfo.getProduct_id(),
        hidDeviceInfo.getSerial_number()
      );

      Preconditions.checkNotNull(device,"Unable to open device");

      log.debug("Selected: {}, {}, {}",
        device.getManufacturerString(),
        device.getProductString(),
        device.getSerialNumberString()
      );

      // Create and configure the USB to UART bridge
      final CP211xBridge uart = new CP211xBridge(device);

      uart.enable(true);
      uart.purge(3);

      // Add unbuffered data streams for easy data manipulation
      out = new DataOutputStream(uart.getOutputStream());
      DataInputStream in = new DataInputStream(uart.getInputStream());

      // Monitor the input stream
      monitorDataInputStream(in);


    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public synchronized void internalClose() {

    Preconditions.checkNotNull(device, "Device is not connected");

    // Attempt to close the socket (also closes the in/out streams)
    try {
      device.close();
      log.info("Disconnected from Trezor");
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private Optional<HIDDeviceInfo> locateTrezor() throws IOException {

    Preconditions.checkState(device == null, "Device is already connected");

    // Get the HID manager
    HIDManager hidManager = HIDManager.getInstance();

    // Attempt to list the attached devices
    HIDDeviceInfo[] infos = hidManager.listDevices();
    if (infos == null) {
      throw new IllegalStateException("Unable to access connected device list. Check USB security policy for this account.");
    }

    Integer vendorId = vendorIdOptional.isPresent() ? vendorIdOptional.get() : DEFAULT_USB_VENDOR_ID;
    Integer productId = productIdOptional.isPresent() ? productIdOptional.get() : DEFAULT_USB_PRODUCT_ID;

    // Attempt to locate the required device
    Optional<HIDDeviceInfo> selectedInfo = Optional.absent();
    for (HIDDeviceInfo info : infos) {
      if (vendorId.equals(info.getVendor_id()) &&
        productId.equals(info.getProduct_id())) {
        // Allow a wildcard serial number
        if (serialNumberOptional.isPresent()) {
          if (serialNumberOptional.get().equals(info.getSerial_number())) {
            selectedInfo = Optional.of(info);
            break;
          }
        } else {
          // Any serial number is acceptable
          selectedInfo = Optional.of(info);
          break;
        }
      }
    }

    return selectedInfo;

  }

  @Override
  public void sendMessage(Message message) throws IOException {

    Preconditions.checkNotNull(message, "Message must be present");
    Preconditions.checkNotNull(device, "Device is not connected");

    // Apply the message to
    writeMessage(message, out);

  }

}