package uk.co.bsol.trezorj.examples.emulators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.bsol.trezorj.core.BlockingTrezorClient;
import uk.co.bsol.trezorj.core.TrezorEvent;
import uk.co.bsol.trezorj.core.emulators.TrezorEmulator;

import java.util.concurrent.TimeUnit;

/**
 * <p>Demonstrates that Java HID API is working on a desktop</p>
 * <p>Just execute {@link TimedEmulatorSocketExample#main(String[])}</p>
 *
 * @since 0.0.1
 *         
 */
public class TimedEmulatorSocketExample {

  private static final Logger log = LoggerFactory.getLogger(TimedEmulatorSocketExample.class);

  /**
   * Entry point to the example
   *
   * @param args No arguments
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // Create the Trezor emulator and start serving
    TrezorEmulator emulator = TrezorEmulator.newDefaultTrezorEmulator();
    emulator.start();

    // Allow time for the emulator to start
    Thread.sleep(100);

    // Create the socket trezor
    BlockingTrezorClient client = BlockingTrezorClient
      .newSocketInstance(
        "localhost",
        3000,
        BlockingTrezorClient.newSessionId()
      );
    client.connect();

    TrezorEvent event1 =  client.getTrezorEventQueue().poll(1, TimeUnit.SECONDS);
    log.info("Received: {} ", event1.eventType());

    TrezorEvent event2 = client.ping();
    log.info("Received: {} {} ", event2.eventType(), event2.protocolMessageType().get());

    System.exit(0);

  }

}