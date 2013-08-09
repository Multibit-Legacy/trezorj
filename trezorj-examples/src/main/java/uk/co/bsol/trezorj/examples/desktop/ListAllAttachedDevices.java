package uk.co.bsol.trezorj.examples.desktop;

import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    ClassPathLibraryLoader.loadNativeHIDLibrary();

    // Get the HID manager
    HIDManager hidManager = HIDManager.getInstance();

    // List the attached devices
    HIDDeviceInfo[] infos = hidManager.listDevices();
    for (HIDDeviceInfo info : infos) {
      log.info("Attached device info: " + info.toString());
    }

    // Finished
    hidManager.release();

  }
}