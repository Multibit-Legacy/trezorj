package uk.co.bsol.trezorj.core.clients;

import com.google.common.base.Optional;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import uk.co.bsol.trezorj.core.Trezor;
import uk.co.bsol.trezorj.core.TrezorFactory;

import java.util.UUID;

/**
 * <p>Factory to provide the following to API consumers:</p>
 * <ul>
 * <li>Creation of standard Trezor clients</li>
 * </ul>
 *
 * <h3>Example 1: Non-blocking (recommended):</h3>
 *
 * <pre>
 * // Create a socket based Trezor client with non-blocking methods (useful production client)
 * NonBlockingTrezorClient client = TrezorClients.newNonBlockingSocketInstance(host, port, TrezorClients.newSessionId());
 *
 * // Connect the client
 * client.connect();
 *
 * // Send a ping
 * client.ping();
 *
 * // Initialize
 * client.initialize();
 *
 * // React to events
 * client.getTrezorEventQueue().poll(1,TimeUnit.SECONDS);
 *
 * // Finish
 * client.close();
 *
 *
 * </pre>

 * <h3>Example 2: Blocking (development):</h3>

 * <pre>
 * // Create a socket based Trezor client with blocking methods (useful for stepping through workflows)
 * BlockingTrezorClient client = TrezorClients.newBlockingSocketInstance(host, port, TrezorClients.newSessionId());
 *
 * // Connect the client
 * TrezorEvent event1 = client.connect();
 *
 * // Send a ping
 * TrezorEvent event2 = client.ping();
 *
 * // Initialize
 * TrezorEvent event3 = client.initialize();
 *
 * // Finish
 * client.close();
 * </pre>
 *
 * @since 0.0.1
 *         
 */
public class TrezorClients {

  /**
   * @return The session ID
   */
  public static ByteString newSessionId() {
    return ByteString.copyFrom(Longs.toByteArray(UUID.randomUUID().getLeastSignificantBits()));
  }

  /**
   * <p>Convenience method to wrap a socket Trezor</p>
   *
   * @param host      The host (e.g. "localhost" or "192.168.0.1")
   * @param port      The port (e.g. 3000)
   * @param sessionId The session ID (typically from {@link TrezorClients#newSessionId()})
   *
   * @return A blocking Trezor client instance with a unique session ID
   */
  public static BlockingTrezorClient newBlockingSocketInstance(String host, int port, ByteString sessionId) {

    // Create a socket Trezor
    Trezor trezor = TrezorFactory.newSocketTrezor(host, port);

    BlockingTrezorClient trezorClient = new BlockingTrezorClient(trezor, sessionId);

    // Add this as the listener (sets the event queue)
    trezor.addListener(trezorClient);

    // Return the new client
    return trezorClient;

  }

  /**
   * <p>Convenience method to wrap a standard USB Trezor (the normal mode of operation)</p>
   *
   * @param sessionId The session ID (typically from {@link TrezorClients#newSessionId()})
   *
   * @return A blocking Trezor client instance with a unique session ID
   */
  public static BlockingTrezorClient newBlockingtUsbInstance(ByteString sessionId) {

    // Create a USB Trezor
    Trezor trezor = TrezorFactory.newUsbTrezor(
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );

    BlockingTrezorClient trezorClient = new BlockingTrezorClient(trezor, sessionId);

    // Add this as the listener (sets the event queue)
    trezor.addListener(trezorClient);

    // Return the new client
    return trezorClient;
  }

  /**
   * <p>Convenience method to wrap a standard USB Trezor (the normal mode of operation)</p>
   *
   * @param vendorIdOptional     The vendor ID (uses default if absent)
   * @param productIdOptional    The product ID (uses default if absent)
   * @param serialNumberOptional The device serial number (accepts any if absent)
   * @param sessionId            The session ID (typically from {@link TrezorClients#newSessionId()})
   *
   * @return A blocking Trezor client instance with a unique session ID
   */
  public static BlockingTrezorClient newBlockingUsbInstance(
    Optional<Integer> vendorIdOptional,
    Optional<Integer> productIdOptional,
    Optional<String> serialNumberOptional,
    ByteString sessionId
  ) {

    // Create a USB Trezor
    Trezor trezor = TrezorFactory.newUsbTrezor(vendorIdOptional, productIdOptional, serialNumberOptional);

    BlockingTrezorClient trezorClient = new BlockingTrezorClient(trezor, sessionId);

    // Add this as the listener (sets the event queue)
    trezor.addListener(trezorClient);

    // Return the new client
    return trezorClient;
  }

  /**
   * <p>Convenience method to wrap a socket Trezor</p>
   *
   * @param host      The host (e.g. "localhost" or "192.168.0.1")
   * @param port      The port (e.g. 3000)
   * @param sessionId The session ID (typically from {@link TrezorClients#newSessionId()})
   *
   * @return A non-blocking Trezor client instance with a unique session ID
   */
  public static NonBlockingTrezorClient newNonBlockingSocketInstance(String host, int port, ByteString sessionId) {

    // Create a socket Trezor
    Trezor trezor = TrezorFactory.newSocketTrezor(host, port);

    NonBlockingTrezorClient trezorClient = new NonBlockingTrezorClient(trezor, sessionId);

    // Add this as the listener (sets the event queue)
    trezor.addListener(trezorClient);

    // Return the new client
    return trezorClient;

  }

  /**
   * <p>Convenience method to wrap a standard USB Trezor (the normal mode of operation)</p>
   *
   * @param sessionId The session ID (typically from {@link TrezorClients##newSessionId()})
   *
   * @return A non-blocking Trezor client instance with a unique session ID
   */
  public static NonBlockingTrezorClient newNonBlockingtUsbInstance(ByteString sessionId) {

    // Create a USB Trezor
    Trezor trezor = TrezorFactory.newUsbTrezor(
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );

    NonBlockingTrezorClient trezorClient = new NonBlockingTrezorClient(trezor, sessionId);

    // Add this as the listener (sets the event queue)
    trezor.addListener(trezorClient);

    // Return the new client
    return trezorClient;
  }

  /**
   * <p>Convenience method to wrap a standard USB Trezor (the normal mode of operation)</p>
   *
   * @param vendorIdOptional     The vendor ID (uses default if absent)
   * @param productIdOptional    The product ID (uses default if absent)
   * @param serialNumberOptional The device serial number (accepts any if absent)
   * @param sessionId            The session ID (typically from {@link #newSessionId()})
   *
   * @return A non-blocking Trezor client instance with a unique session ID
   */
  public static NonBlockingTrezorClient newNonBlockingUsbInstance(
    Optional<Integer> vendorIdOptional,
    Optional<Integer> productIdOptional,
    Optional<String> serialNumberOptional,
    ByteString sessionId
  ) {

    // Create a USB Trezor
    Trezor trezor = TrezorFactory.newUsbTrezor(vendorIdOptional, productIdOptional, serialNumberOptional);

    NonBlockingTrezorClient trezorClient = new NonBlockingTrezorClient(trezor, sessionId);

    // Add this as the listener (sets the event queue)
    trezor.addListener(trezorClient);

    // Return the new client
    return trezorClient;
  }

}
