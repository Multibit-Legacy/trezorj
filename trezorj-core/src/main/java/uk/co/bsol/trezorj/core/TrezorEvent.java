package uk.co.bsol.trezorj.core;

import com.google.common.base.Optional;
import com.google.protobuf.AbstractMessage;
import uk.co.bsol.trezorj.core.protobuf.MessageType;

/**
 * <p>Interface to provide the following to application:</p>
 * <ul>
 * <li>Identification of the underlying protocol buffer message for an event</li>
 * <li>The event type</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public interface TrezorEvent {

  /**
   * @return The protocol buffer message that backs this event (if present)
   */
  Optional<AbstractMessage> trezorMessage();

  /**
   * @return The message type
   */
  MessageType messageType();
}
