package uk.co.bsol.trezorj.core.trezors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.bsol.trezorj.core.Trezor;
import uk.co.bsol.trezorj.core.TrezorEvent;
import uk.co.bsol.trezorj.core.TrezorEventType;
import uk.co.bsol.trezorj.core.TrezorListener;
import uk.co.bsol.trezorj.core.events.TrezorEvents;
import uk.co.bsol.trezorj.core.protobuf.MessageType;
import uk.co.bsol.trezorj.core.protobuf.TrezorMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>Abstract base class to provide the following to Trezor devices:</p>
 * <ul>
 * <li>Access to common methods</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public abstract class AbstractTrezor implements Trezor {

  private static final Logger log = LoggerFactory.getLogger(AbstractTrezor.class);

  private static final int MAX_QUEUE_SIZE = 32;

  protected final Set<TrezorListener> listeners = Sets.newLinkedHashSet();

  private final ExecutorService trezorEventExecutorService = Executors.newSingleThreadExecutor();

  @Override
  public synchronized void addListener(TrezorListener trezorListener) {

    Preconditions.checkState(listeners.add(trezorListener), "Listener is already present");

    // Create a new queue for events
    BlockingQueue<TrezorEvent> listenerQueue = Queues.newArrayBlockingQueue(MAX_QUEUE_SIZE);
    trezorListener.setTrezorEventQueue(listenerQueue);
  }

  @Override
  public synchronized void removeListener(TrezorListener trezorListener) {

    Preconditions.checkState(listeners.remove(trezorListener), "Listener was not present");

    // Remove the queue
    trezorListener.setTrezorEventQueue(null);
  }

  /**
   * <p>Create an executor service to monitor the data input stream and raise events</p>
   */
  protected void monitorDataInputStream(final DataInputStream in) {

    trezorEventExecutorService.submit(new Runnable() {
      @Override
      public void run() {

        while (true) {
          try {
            // Read a message (blocking)
            final TrezorEvent trezorEvent = readMessage(in);

            log.debug("Firing event: {} ", trezorEvent.eventType().name());
            for (TrezorListener listener : listeners) {
              listener.getTrezorEventQueue().put(trezorEvent);
            }

            Thread.sleep(100);

          } catch (EOFException e) {
            // Do nothing
          } catch (InterruptedException e) {
            break;
          } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
          }
        }

      }
    });
  }

  /**
   * <p>Blocking method to read from the data input stream</p>
   *
   * @param in The data input stream (must be open)
   * @return The expected protocol buffer message for the detail
   * @throws IOException If something goes wrong
   */
  private synchronized TrezorEvent readMessage(DataInputStream in) throws IOException {

    // Very broad try-catch because a lot of things can go wrong here and need to be reported
    try {

      // Read and throw away the magic header markers
      in.readByte();
      in.readByte();

      // Read the header code and select a suitable parser
      final Short headerCode = in.readShort();
      final MessageType messageType = MessageType.getMessageTypeByHeaderCode(headerCode);
      final Parser parser = MessageType.getParserByHeaderCode(headerCode);

      // Read the detail length
      final int detailLength = in.readInt();

      // Read the remaining bytes
      final byte[] detail = new byte[detailLength];
      final int actualLength = in.read(detail, 0, detailLength);

      // Verify the read
      Preconditions.checkState(actualLength == detailLength, "Detail not read fully. Expected=" + detailLength + " Actual=" + actualLength);

      // Parse the detail into a message

      final AbstractMessage message = (AbstractMessage) parser.parseFrom(detail);
      log.debug("< {}", message.getClass().getName());

      if (MessageType.FAILURE.equals(messageType)) {
        log.error("FAILED: {}", ((TrezorMessage.Failure) message).getMessage());
      }

      // Build the event from the given information
      return TrezorEvents.newProtocolEvent(messageType, message);

    } catch (EOFException e) {
      return TrezorEvents.newSystemEvent(TrezorEventType.DEVICE_EOF);
    } catch (Throwable e) {
      log.error(e.getMessage(),e);
      return TrezorEvents.newSystemEvent(TrezorEventType.DEVICE_FAILURE);
    }

  }

  /**
   * @param message The protocol buffer message to read
   * @param out     The data output stream (must be open)
   * @throws IOException If something goes wrong
   */
  protected void writeMessage(AbstractMessage message, DataOutputStream out) throws IOException {

    // Require the header code
    short headerCode = MessageType.getHeaderCode(message);

    // Provide some debugging
    MessageType messageType = MessageType.getMessageTypeByHeaderCode(headerCode);
    log.debug("> {}", messageType.name());

    // Write magic alignment string (avoiding immediate flush)
    out.writeBytes("##");

    // Write header following Python's ">HL" syntax
    // > = Big endian, std size and alignment
    // H = Unsigned short (2 bytes) for header code
    // L = Unsigned long (4 bytes) for message length

    // Message type
    out.writeShort(headerCode);

    // Message length
    out.writeInt(message.getSerializedSize());

    // Write the detail portion as a protocol buffer message
    message.writeTo(out);

    // Flush to ensure bytes are available immediately
    out.flush();
  }

  @Override
  public synchronized void close() {

    internalClose();
    trezorEventExecutorService.shutdownNow();

  }

  /**
   * <p>Implementations should handle their own shutdown before their threads are terminated</p>
   */
  public abstract void internalClose();

}
