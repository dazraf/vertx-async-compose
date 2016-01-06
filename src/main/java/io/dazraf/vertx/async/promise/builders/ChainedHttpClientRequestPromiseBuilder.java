package io.dazraf.vertx.async.promise.builders;

import io.dazraf.vertx.async.promise.Promise;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

import java.util.function.BiFunction;
import java.util.function.Function;

import static io.dazraf.vertx.async.promise.builders.AsyncResultWrapper.failed;
import static io.dazraf.vertx.async.promise.builders.AsyncResultWrapper.success;

public class ChainedHttpClientRequestPromiseBuilder<PreviousResult> {
  private final Promise<PreviousResult> previousPromise
    ;

  public ChainedHttpClientRequestPromiseBuilder(Promise<PreviousResult> previousPromise) {
    this.previousPromise = previousPromise;
  }


  public Promise<HttpClientResponse> then(Function<Handler<HttpClientResponse>, HttpClientRequest> implementation) {
    return callback -> {
      this.previousPromise.execute(asyncResult -> {
        if (asyncResult.succeeded()) {
          try {
            implementation.apply(result -> {
              callback.handle(success(result));
            })
              .exceptionHandler(throwable -> callback.handle(failed(throwable)))
              .end();
          } catch (Throwable throwable) {
            callback.handle(failed(throwable));
          }
        } else {
          callback.handle(failed(asyncResult.cause()));
        }
      });
    };
  }

  public Promise<HttpClientResponse> then(BiFunction<AsyncResult<PreviousResult>, Handler<HttpClientResponse>, HttpClientRequest> implementation) {
    return callback -> {
      this.previousPromise.execute(asyncResult -> {
          try {
            implementation.apply(asyncResult, result -> {
              callback.handle(success(result));
            })
              .exceptionHandler(throwable -> callback.handle(failed(throwable)))
              .end();
          } catch (Throwable throwable) {
            callback.handle(failed(throwable));
          }
      });
    };
  }
}
