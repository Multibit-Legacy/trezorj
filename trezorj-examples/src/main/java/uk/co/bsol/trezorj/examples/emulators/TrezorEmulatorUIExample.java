package uk.co.bsol.trezorj.examples.emulators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.bsol.trezorj.core.BlockingTrezorClient;
import uk.co.bsol.trezorj.core.TrezorEvent;
import uk.co.bsol.trezorj.core.emulators.TrezorEmulator;
import uk.co.bsol.trezorj.core.emulators.TrezorEmulatorUI;
import uk.co.bsol.trezorj.core.protobuf.TrezorMessage;

import java.util.concurrent.TimeUnit;

/**
 * <p>Demonstrates the TrezorEmulatorUI running on a desktop</p>
 * <p>Just execute {@link uk.co.bsol.trezorj.examples.emulators.TrezorEmulatorUIExample#main(String[])}</p>
 *
 * @since 0.0.1
 *         
 */
public class TrezorEmulatorUIExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorEmulatorUIExample.class);

  /**
   * Entry point to the example
   *
   * @param args No arguments
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // Create and show the Trezor emulator UI.
    // This creates internally a TrezorEmulator but does not start it.
    TrezorEmulatorUI trezorEmulatorUI = new TrezorEmulatorUI(true);

    TrezorEmulator trezorEmulator = TrezorEmulatorUI.getTrezorEmulator();

    // Act out a dialog between this class and the TrezorEmulator as follows:
    // Time(ms)    Actor             Message
    // --------    -----             -------
    // 0           trezor            start up
    // 100         this              connect
    // 1000        trezor            SUCCESS  << Added at construction

    // 1500        this              ping
    // 2000        trezor            SUCCESS

    // 2500        this              initialise <session_id>
    // 3000        trezor            as below
    // Features session_id: "\225\342J\205\322\3560\242"
    //  vendor: "bitcointrezor.com"
    //  major_version: 0
    //  minor_version: 1
    //  has_otp: false
    //  has_spv: false
    //  pin: true
    //  algo: ELECTRUM
    //  algo_available: ELECTRUM
    //  maxfee_kb: 1000000

    // 3500        this              close
    // 4000        trezor            DEVICE_DISCONNECTED

    // ping reply
    trezorEmulator.addMessage(new TrezorEmulator.EmulatorMessage(
              TrezorMessage.Success
                      .newBuilder()
                      .setMessage("")
                      .build(),
              2000, TimeUnit.MILLISECONDS
      ));


    // Start the emulator - the canned conversation then starts.
    TrezorEmulatorUI.startEmulator();


    // Allow time for the emulator to start
    Thread.sleep(100);
    // Send connect

    Thread.sleep(1400);
    // Send ping

    Thread.sleep(1000);
    // Send close


    // Wait for the dialog to run its course before closing
    Thread.sleep(10000);
    System.exit(0);

  }
}