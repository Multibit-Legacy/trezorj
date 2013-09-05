package uk.co.bsol.trezorj.core.trezors;

import org.junit.Test;
import uk.co.bsol.trezorj.core.TrezorEvent;
import uk.co.bsol.trezorj.core.TrezorFactory;
import uk.co.bsol.trezorj.core.TrezorListener;
import uk.co.bsol.trezorj.core.protobuf.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class SocketTrezorTest {

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
          socket.getOutputStream().write(new byte[]{33, 33, 0, 2, 0, 0, 0, 0});

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
      assertThat(event1.messageType()).isNotNull();
      assertThat(event1.messageType()).isEqualTo(MessageType.SUCCESS);

      testObject.close();

    } catch (TimeoutException e) {
      fail("Socket failed to connect", e);
    }


  }
}
