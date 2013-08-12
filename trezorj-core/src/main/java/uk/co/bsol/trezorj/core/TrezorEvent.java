package uk.co.bsol.trezorj.core;

import com.google.common.base.Optional;
import com.google.protobuf.AbstractMessage;

/**
 * <p>Interface to provide the following to application:</p>
 * <ul>
 * <li>Comment event methods</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public interface TrezorEvent {

  /**
   * @return The protocol buffer message that backs this event (if present)
   */
  Optional<AbstractMessage> originatingMessage();

}
