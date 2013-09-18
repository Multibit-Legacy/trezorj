package uk.co.bsol.trezorj.core.trezors;

import com.google.protobuf.Message;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.bsol.trezorj.core.TrezorEvent;
import uk.co.bsol.trezorj.core.TrezorFactory;
import uk.co.bsol.trezorj.core.TrezorListener;
import uk.co.bsol.trezorj.core.protobuf.MessageType;
import uk.co.bsol.trezorj.core.protobuf.TrezorMessage;
import uk.co.bsol.trezorj.core.utils.TrezorMessageUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class SocketTrezorTest {
    private static final Logger log = LoggerFactory.getLogger(SocketTrezorTest.class);

    @Test
  public void testConnect() throws Exception {

    // Arrange
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    TrezorListener listener = new TrezorListener() {

      private BlockingQueue<TrezorEvent> queue;

      @Override
      public BlockingQueue<TrezorEvent> getTrezorEventQueue() {
        return this.queue;
      }

      @Override
      public void setTrezorEventQueue(BlockingQueue<TrezorEvent> trezorEventQueue) {
        this.queue = trezorEventQueue;
      }
    };

    Future<Boolean> futureResult = executorService.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() {

        try {
          ServerSocket serverSocket = new ServerSocket(3000);

          // Block until a connection is attempted
          Socket socket = serverSocket.accept();

          // Send some data (a Trezor SUCCESS response)
          Message message = TrezorMessage.Success.newBuilder().setMessage("").build();
          DataOutputStream out = new DataOutputStream(socket.getOutputStream());
          TrezorMessageUtils.writeMessage(message, out);

          // A connection has been made
          return true;
        } catch (IOException e) {
          return false;
        }
      }
    });

    SocketTrezor testObject = TrezorFactory.newSocketTrezor("localhost", 3000);

    // Act
    testObject.addListener(listener);
    testObject.connect();

    // Assert
    try {
      // Expect a result within a short time
      Boolean result = futureResult.get(1, TimeUnit.SECONDS);
      assertThat(result).isTrue();

      // Verify that the data was read in correctly
      TrezorEvent event1 = listener.getTrezorEventQueue().poll(1, TimeUnit.SECONDS);
      assertThat(event1).isNotNull();
      assertThat(event1.protocolMessageType().isPresent()).isTrue();
      assertThat(event1.protocolMessageType().get()).isEqualTo(MessageType.SUCCESS);

      testObject.close();

    } catch (TimeoutException e) {
      fail("Socket failed to connect", e);
    }


  }
}
