package io.dazraf.vertx.async.promise.builders;

import io.dazraf.vertx.async.promise.Promise;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChainedPromiseBuilder<PreviousResult, Result> {

  private final Promise<PreviousResult> previous;

  public ChainedPromiseBuilder(Promise<PreviousResult> previousPromise) {
    this.previous = previousPromise;
  }

  @SuppressWarnings("unchecked")
  public Promise<Result> invoke(Consumer<Handler<AsyncResult<Result>>> implementation) {
    return callback -> previous.execute(asyncResult -> {
       if (asyncResult.succeeded()) {
         implementation.accept(callback);
       } else {
         callback.handle((AsyncResult<Result>) asyncResult);
       }
    });
  }

  @SuppressWarnings("unchecked")
  public Promise<Result> invoke(BiConsumer<AsyncResult<PreviousResult>, Handler<AsyncResult<Result>>> implementation) {
    return callback -> previous.execute(asyncResult -> implementation.accept(asyncResult, callback));
  }

}
