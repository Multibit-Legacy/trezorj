package uk.co.bsol.trezorj.core;

import com.google.protobuf.AbstractMessage;

import java.io.IOException;

/**
 * <p>Interface to provide the following to applications:</p>
 * <ul>
 * <li>Common methods available to different Trezor devices</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public interface Trezor {

  /**
   * <p>Called to attempt a connection to the device</p>
   */
  void connect();

  /**
   * <p>Called to break the connection to the device</p>
   */
  void close();

  /**
   * <p>Send a message to the device using the generated protocol buffer classes</p>
   * <p>Any response will be provided through the listener interface</p>
   *
   * @param message A generated protocol buffer message (e.g. Message.Initialize)
   *
   * @throws IOException If something goes wrong
   */
  void sendMessage(AbstractMessage message) throws IOException;

  /**
   * <p>Add a Trezor listener - duplicates will be rejected</p>
   *
   * @param trezorListener A Trezor listener
   */
  void addListener(TrezorListener trezorListener);

  /**
   * <p>Remove a Trezor listener - </p>
   *
   * @param trezorListener A Trezor listener
   */
  void removeListener(TrezorListener trezorListener);

}
