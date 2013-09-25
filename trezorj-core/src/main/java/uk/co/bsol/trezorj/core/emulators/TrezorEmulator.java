package uk.co.bsol.trezorj.core.emulators;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.bsol.trezorj.core.protobuf.TrezorMessage;
import uk.co.bsol.trezorj.core.utils.TrezorMessageUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>Trezor emulator to provide the following to applications:</p>
 * <ul>
 * <li>A programmable Trezor emulator to offer up a sequence of protocol buffer messages</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public class TrezorEmulator {

  private static final Logger log = LoggerFactory.getLogger(TrezorEmulator.class);

  public static final String DEFAULT_SOCKET_HOST = "localhost";
  public static final int DEFAULT_SOCKET_PORT = 3000;

  private final List<EmulatorMessage> messages = Lists.newArrayList();

  private final Optional<DataOutputStream> outputStreamOptional;

  // Not used yet as input stream is not listened to
  private final Optional<DataInputStream> inputStreamOptional;

  // True if the emulator has been fully configured
  private boolean isBuilt = false;
  private ExecutorService executorService;
  private final ServerSocket serverSocket;

  /**
   * <p>Utility method to provide a common sequence</p>
   * This emulator transmits over a socket - port 3000
   *
   * @return A default Trezor emulator with simple timed responses
   *
   * @throws IOException If something goes wrong
   */
  public static TrezorEmulator newDefaultTrezorEmulator() throws IOException {

    TrezorEmulator emulator = new TrezorEmulator(Optional.<DataOutputStream>absent(), Optional.<DataInputStream>absent());
    addSuccessMessage(emulator, 100, TimeUnit.MILLISECONDS);

    return emulator;

  }

  /**
   * <p>Utility method to provide a common sequence</p>
   * This emulator sends and receives over streams
   *
   * @param transmitStream The stream the emulator will transmit replies to
   * @param receiveStream  The stream the emulator will receive data on
   *
   * @return A default Trezor emulator with simple timed responses
   *
   * @throws IOException If something goes wrong
   */
  public static TrezorEmulator newStreamingTrezorEmulator(OutputStream transmitStream, InputStream receiveStream) throws IOException {

    Preconditions.checkNotNull(transmitStream, "'transmitStream' must be present");
    // TODO Re-instate this check
    // Preconditions.checkNotNull(receiveStream,"'receiveStream' must be present");

    TrezorEmulator emulator = new TrezorEmulator(
      Optional.of(new DataOutputStream(transmitStream)),
      Optional.<DataInputStream>absent()
    );
    addSuccessMessage(emulator, 1, TimeUnit.SECONDS);

    return emulator;

  }

  public static void addSuccessMessage(TrezorEmulator trezorEmulator, int duration, TimeUnit timeUnit) {
    trezorEmulator.addMessage(new EmulatorMessage(
      TrezorMessage.Success
        .newBuilder()
        .setMessage("")
        .build(),
      duration,
      timeUnit
    ));
  }

  /**
   * Use the utility constructors
   */
  private TrezorEmulator(Optional<DataOutputStream> outputStreamOptional, Optional<DataInputStream> inputStreamOptional) throws IOException {
    this.outputStreamOptional = outputStreamOptional;
    this.inputStreamOptional = inputStreamOptional;
    this.serverSocket = new ServerSocket(DEFAULT_SOCKET_PORT);
  }


  /**
   * <p>Add a new emulator message to the queue</p>
   *
   * @param emulatorMessage The emulator message to add
   */
  public void addMessage(EmulatorMessage emulatorMessage) {

    validateState();

    log.debug("Adding '{}'", emulatorMessage.getTrezorMessage());

    messages.add(emulatorMessage);

  }

  /**
   * <p>Start the emulation process</p>
   */
  public void start() throws ExecutionException, InterruptedException {

    // Prevent further modifications
    isBuilt = true;

    log.debug("Starting emulator");

    // Arrange
    executorService = Executors.newSingleThreadExecutor();

    executorService.submit(new Runnable() {
      @Override
      public void run() {

        try {

          final DataOutputStream out;

          if (!outputStreamOptional.isPresent()) {

            log.debug("Accepting connections on socket configured on port {}",serverSocket.getLocalPort());

            // Block until a connection is attempted
            Socket socket = serverSocket.accept();

            log.debug("Connected. Starting message sequence.");

            out = new DataOutputStream(socket.getOutputStream());
          } else {
            log.debug("Transmitting over a data stream");
            out = outputStreamOptional.get();
          }

          // Work through the emulator messages
          for (EmulatorMessage message : messages) {

            long millis = message.getTimeUnit().toMillis(message.getDuration());

            log.debug("Sleeping {}ms", millis);

            // Wait for the required period of time
            Thread.sleep(millis);

            log.debug("Emulating '{}'", message.getTrezorMessage());

            TrezorMessageUtils.writeMessage(message.getTrezorMessage(), out);

          }

          // All messages complete so free up resources
          stop();

        } catch (IOException e) {
          log.error(e.getMessage(), e);
        } catch (InterruptedException e) {
          log.error(e.getMessage(), e);
        }
      }
    });

  }

  /**
   * Stop the emulator and clean up all threads
   */
  public void stop() {

    log.debug("Stopping");

    try {
      serverSocket.close();
      executorService.shutdownNow();

      // Prevent collisions during shutdown
      log.debug("Waiting for shutdown");
      Thread.sleep(200);
    } catch (IOException e) {
      log.error("Emulator socket failed to close", e);
    } catch (InterruptedException e) {
      // The synchronization has done its job
    }

  }

  private void validateState() {
    if (isBuilt) {
      throw new IllegalStateException("Emulator is already built");
    }
  }

  /**
   * Immutable object representing a single Trezor message (usually a timed response)
   */
  public static class EmulatorMessage {

    private final Message trezorMessage;
    private final int duration;
    private final TimeUnit timeUnit;

    /**
     * @param trezorMessage The Trezor protobuf message
     * @param duration      The duration to wait before triggering the message
     * @param timeUnit      The time unit
     */
    public EmulatorMessage(Message trezorMessage, int duration, TimeUnit timeUnit) {
      this.trezorMessage = trezorMessage;
      this.duration = duration;
      this.timeUnit = timeUnit;
    }

    private Message getTrezorMessage() {
      return trezorMessage;
    }

    private int getDuration() {
      return duration;
    }

    private TimeUnit getTimeUnit() {
      return timeUnit;
    }
  }
}
