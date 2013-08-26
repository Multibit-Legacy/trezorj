package uk.co.bsol.trezorj.core;

import uk.co.bsol.trezorj.core.trezors.SocketTrezor;

/**
 * <p>Factory to provide the following to applications:</p>
 * <ul>
 * <li>Access to Trezor devices over different communication links</li>
 * </ul>
 * <p>Example:</p>
 * <pre>
 *   SocketTrezor trezor = TrezorFactory.INSTANCE.newSocketTrezor("192.168.0.1",3000)
 * </pre>
 * <p>See the trezorj-examples module for more comprehensive examples</p>
 *
 * @since 0.0.1
 *         
 */
public enum TrezorFactory {

  INSTANCE

  // End of enum
  ;

  /**
   *
   * @param host The host  (e.g. "localhost", "192.168.0.1" etc)
   * @param port The port (e.g. 3000)
   * @return A socket based Trezor
   */
  public SocketTrezor newSocketTrezor(String host, int port) {

    return new SocketTrezor(host, port);

  }

}
