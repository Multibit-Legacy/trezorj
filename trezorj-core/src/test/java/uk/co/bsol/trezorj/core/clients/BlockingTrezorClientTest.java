package uk.co.bsol.trezorj.core.clients;

import com.google.protobuf.ByteString;
import org.junit.Test;
import uk.co.bsol.trezorj.core.TrezorEvent;
import uk.co.bsol.trezorj.core.TrezorEventType;
import uk.co.bsol.trezorj.core.emulators.TrezorEmulator;
import uk.co.bsol.trezorj.core.protobuf.MessageType;

import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

public class BlockingTrezorClientTest {

  @Test
  public void testConnect() throws Exception {

    // Arrange
    ByteString sessionId = TrezorClients.newSessionId();

    BlockingTrezorClient testObject = TrezorClients.newBlockingSocketInstance(
      "localhost",
      3000,
      sessionId
    );

    TrezorEmulator emulator = TrezorEmulator.newDefaultTrezorEmulator();
    emulator.start();

    // Act
    testObject.connect();

    // Assert
    // Expect a result within a short time

    // Verify that the device connected
    TrezorEvent event1 = testObject.getTrezorEventQueue().poll(1, TimeUnit.SECONDS);
    assertThat(event1).isNotNull();
    assertThat(event1.protocolMessageType().isPresent()).isFalse();
    assertThat(event1.eventType()).isEqualTo(TrezorEventType.DEVICE_CONNECTED);

    // Verify that the data was read in correctly
    TrezorEvent event2 = testObject.getTrezorEventQueue().poll(1, TimeUnit.SECONDS);
    assertThat(event2).isNotNull();
    assertThat(event2.protocolMessageType().isPresent()).isTrue();
    assertThat(event2.protocolMessageType().get()).isEqualTo(MessageType.SUCCESS);

    testObject.close();

  }
}
