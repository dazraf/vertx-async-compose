package io.dazraf.vertx.async.promise.builders;

import io.dazraf.vertx.async.promise.Promise;
import io.vertx.core.Handler;

import java.util.function.Consumer;

import static io.dazraf.vertx.async.promise.builders.AsyncResultWrapper.failed;
import static io.dazraf.vertx.async.promise.builders.AsyncResultWrapper.success;

public class RawPromiseBuilder<CallbackResultType> {
  public Promise<CallbackResultType> then(Consumer<Handler<CallbackResultType>> implementation) {
    return callback -> {
      try {
        implementation.accept(result -> {
          callback.handle(success(result));
        });
      } catch (Throwable throwable) {
        callback.handle(failed(throwable));
      }
    };
  }
}
