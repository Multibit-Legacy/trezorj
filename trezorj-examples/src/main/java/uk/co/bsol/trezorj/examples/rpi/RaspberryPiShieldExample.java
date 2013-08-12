package uk.co.bsol.trezorj.examples.rpi;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.bsol.trezorj.core.Trezor;
import uk.co.bsol.trezorj.core.TrezorEvent;
import uk.co.bsol.trezorj.core.TrezorFactory;
import uk.co.bsol.trezorj.core.TrezorListener;
import uk.co.bsol.trezorj.core.protobuf.TrezorMessage;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>Example of communicating with a Raspberry Pi Shield Trezor:</p>
 * <h3>How to configure your Raspberry Pi</h3>
 * <p>Change the standard <code>rpi-serial.sh</code> script to use the following:</p>
 * <pre>
 * python trezor/__init__.py -s -t socket -p 0.0.0.0:3000 -d -dt socket -dp 0.0.0.0:2000
 * </pre>
 * <p>This will ensure that the Shield is serving over port 3000 with a debug socket on port 2000</p>
 * <h4>*** Warning *** Do not use this mode with real private keys since it is unsafe!</h4>
 * <h3>How to run this locally</h3>
 * <p>You need to pass in the IP address of your RPi unit (use <code>ip addr</code> at the Rpi terminal to find it
 * listed under <code>eth0: inet a.b.c.d</code></p>
 *
 * @since 0.0.1
 *         
 */
public class RaspberryPiShieldExample implements TrezorListener {

  private static final Logger log = LoggerFactory.getLogger(RaspberryPiShieldExample.class);

  private BlockingQueue<TrezorEvent> trezorEventQueue;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args [0]: IP address of RPi unit (e.g. "192.168.0.1"), [1]: Port (e.g. "3000")
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    Preconditions.checkNotNull(args, "Missing arguments for RPi unit.");
    Preconditions.checkState(args.length == 2, "Required arguments [0]: host name or IP, [1]: port.");

    // All the work is done in the class
    RaspberryPiShieldExample example = new RaspberryPiShieldExample();
    example.executeExample(args[0], Integer.parseInt(args[1]));

  }

  /**
   * @param host The host name or IP (e.g. "192.168.0.1")
   * @param port The port (e.g. 3000)
   *
   * @throws IOException If something goes wrong
   */
  public void executeExample(String host, int port) throws IOException {

    // Create a socket Trezor
    Trezor trezor = TrezorFactory.INSTANCE.newSocketTrezor(host, port);

    // Add this as the listener (sets the event queue)
    trezor.addListener(this);

    // Set up an executor service to monitor Trezor events
    createTrezorEventExecutorService();

    // Connect
    trezor.connect();

    // Send a message
    trezor.sendMessage(TrezorMessage.Ping.getDefaultInstance());

    // The event thread will report any response

  }

  /**
   * The Trezor event monitoring executor service
   */
  private void createTrezorEventExecutorService() {

    ExecutorService trezorEventExecutorService = Executors.newSingleThreadExecutor();
    trezorEventExecutorService.submit(new Runnable() {
      @Override
      public void run() {

        BlockingQueue<TrezorEvent> queue = getTrezorEventQueue();

        while (true) {
          try {
            TrezorEvent event = queue.take();

            log.info("Received event: {}",event.originatingMessage().get().getClass().getName());

          } catch (InterruptedException e) {
            break;
          }
        }

      }
    });
  }

  @Override
  public BlockingQueue<TrezorEvent> getTrezorEventQueue() {
    return trezorEventQueue;
  }

  @Override
  public void setTrezorEventQueue(BlockingQueue<TrezorEvent> trezorEventQueue) {
    this.trezorEventQueue = trezorEventQueue;
  }
}
