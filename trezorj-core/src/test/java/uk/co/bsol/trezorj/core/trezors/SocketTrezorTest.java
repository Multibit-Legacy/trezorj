package uk.co.bsol.trezorj.core.trezors;

import org.junit.Test;
import uk.co.bsol.trezorj.core.TrezorFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.*;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class SocketTrezorTest {

  @Test
  public void testConnect() throws Exception {

    // Arrange
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    Future<Boolean> futureResult = executorService.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() {

        try {
          ServerSocket serverSocket = new ServerSocket(3000);

          // Block until a connection is attempted
          serverSocket.accept();

          // A connection has been made
          return true;
        } catch (IOException e) {
          return false;
        }
      }
    });

    SocketTrezor testObject = TrezorFactory.newSocketTrezor("localhost", 3000);

    // Act
    testObject.connect();

    // Assert
    try {
      // Expect a result within a short time
      Boolean result= futureResult.get(1, TimeUnit.SECONDS);
      assertThat(result).isTrue();
    } catch (TimeoutException e) {
      fail("Socket failed to connect", e);
    }



  }
}
