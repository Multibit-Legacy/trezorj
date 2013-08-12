package uk.co.bsol.trezorj.core;

import uk.co.bsol.trezorj.core.trezors.SocketTrezor;

/**
 * <p>Factory to provide the following to applications:</p>
 * <ul>
 * <li>Access to Trezor devices over different communication links</li>
 * </ul>
 * <p>Example:</p>
 * <pre>
 *   Trezor trezor = TrezorFactory.INSTANCE.newSocketTrezor("192.168.0.1",3000)
 * </pre>
 * <p>See the /examples module for more comprehensive examples</p>
 *
 * @since 0.0.1
 *         
 */
public enum TrezorFactory {

  INSTANCE

  // End of enum
  ;

  public Trezor newSocketTrezor(String host, int port) {

    return new SocketTrezor(host, port);


  }

}
