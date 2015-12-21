package io.dazraf.vertx.async;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static io.dazraf.vertx.async.promise.Promises.all;
import static io.dazraf.vertx.async.promise.Promises.create;

public class PromiseTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(PromiseTest.class);
  public static final String ADDRESS = "address";
  private Vertx vertx = Vertx.vertx();

  @Test
  public void test() throws InterruptedException {

    // let's register for some message handling off the event bus
    vertx.eventBus().consumer(ADDRESS, (Message<JsonObject> m) -> {
      LOGGER.info("consumer received: {}", m.body().encodePrettily());
      m.reply(m.body());
    });

    final CountDownLatch latch = new CountDownLatch(1);

    // lets run taks '1', '2' in parallel
    all(
      // the general form is: async return type, function to call, parameters
      create(JsonObject.class, vertx.eventBus()::send, ADDRESS, createJsonObjectWithId(1)).peek(this::writeResponse),
      create(JsonObject.class, vertx.eventBus()::send, ADDRESS, createJsonObjectWithId(2)).peek(this::writeResponse)
    )
      // here we wait for '1' and '2' to finish before running '3'
      .then(JsonObject.class, vertx.eventBus()::send, ADDRESS, createJsonObjectWithId(3)).peek(this::writeResponse)
      .thenAll(
        // here we execute in parallel again, this time for '4' and '5'
        create(JsonObject.class, vertx.eventBus()::send, ADDRESS, createJsonObjectWithId(4)).peek(this::writeResponse),
        create(JsonObject.class, vertx.eventBus()::send, ADDRESS, createJsonObjectWithId(5)).peek(this::writeResponse)
      )
      // we now execute the entire built pipeline, capturing the state of the last task
      .eval(ar -> {
        latch.countDown();
        LOGGER.info("all done");
      });

    // let's wait for everythign to finish
    latch.await();
  }

  private void writeResponse(AsyncResult<Message<JsonObject>> asyncResult) {
    LOGGER.info("success: {} value: {}", asyncResult.succeeded(), asyncResult.succeeded() ? asyncResult.result().body().encodePrettily() : "null");
  }

  private JsonObject createJsonObjectWithId(int id) {
    return new JsonObject().put("id", id);
  }
}
