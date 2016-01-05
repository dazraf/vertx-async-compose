package io.dazraf.vertx.async.promise.builders;

import io.dazraf.vertx.async.promise.Promise;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.dazraf.vertx.async.promise.builders.AsyncResultWrapper.failed;
import static io.dazraf.vertx.async.promise.builders.AsyncResultWrapper.success;

public class ChainedRawPromiseBuilder<PreviousResult, Result> {
  private final Promise<PreviousResult> previousPromise;

  public ChainedRawPromiseBuilder(Promise<PreviousResult> previousPromise) {
    this.previousPromise = previousPromise;
  }

  @SuppressWarnings("unchecked")
  public Promise<Result> invoke(Consumer<Handler<Result>> implementation) {
    return callback ->
      previousPromise.execute(asyncResult -> {
        if (asyncResult.succeeded()) {
          try {
            implementation.accept(result -> {
              callback.handle(success(result));
            });
          } catch (Throwable throwable) {
            callback.handle(failed(throwable));
          }
        } else {
          callback.handle((AsyncResult<Result>) asyncResult);
        }
      });
  }

  @SuppressWarnings("unchecked")
  public Promise<Result> invoke(BiConsumer<AsyncResult<PreviousResult>, Handler<Result>> implementation) {
    return callback -> {
      previousPromise.execute(asyncResult -> {
        try {
          implementation.accept(asyncResult, result -> {
            callback.handle(success(result));
          });
        } catch (Throwable throwable) {
          callback.handle(failed(throwable));
        }
      });
    };
  }
}
