package move.action;

import io.vertx.rxjava.core.Vertx;

/**
 * Created by clay on 6/10/17.
 */
public class WsClient {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.createHttpClient().websocket(9001, "localhost", "/",
        event -> {
          event.textMessageHandler(
              message -> {
                System.out.println(message);
              }
          );

          event.writeFinalTextFrame("PING");
        },
        event -> {
          event.printStackTrace();
        }
    );
  }
}
