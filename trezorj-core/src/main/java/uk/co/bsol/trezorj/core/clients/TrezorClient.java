package uk.co.bsol.trezorj.core.clients;

import uk.co.bsol.trezorj.core.TrezorEvent;
import uk.co.bsol.trezorj.core.TrezorListener;

import java.util.concurrent.BlockingQueue;

/**
 * <p>Interface to provide the following to API consumers:</p>
 * <ul>
 * <li>Provision of a standard set of methods for interacting with a Trezor device</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public interface TrezorClient extends TrezorListener {
  /**
   * <p>Connect to the Trezor device. No initialization takes place.</p>
   */
  void connect();

  /**
   * <p>Close the connection to the Trezor device. This client instance can no longer be used.</p>
   */
  void close();

  @Override
  BlockingQueue<TrezorEvent> getTrezorEventQueue();

  @Override
  void setTrezorEventQueue(BlockingQueue<TrezorEvent> trezorEventQueue);
}
