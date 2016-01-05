package io.dazraf.vertx.async.promise.builders;

import io.dazraf.vertx.async.promise.Promise;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

import java.util.function.Function;

public class HttpClientRequestPromiseBuilder {
  public Promise<HttpClientResponse> invoke(Function<Handler<HttpClientResponse>, HttpClientRequest> implementation) {
    return callback -> {
      try {
        implementation.apply(result -> {
          callback.handle(AsyncResultWrapper.success(result));
        }).exceptionHandler(throwable -> {
          callback.handle(AsyncResultWrapper.failed(throwable));
        }).end();
      } catch (Throwable throwable) {
        callback.handle(AsyncResultWrapper.failed(throwable));
      }
    };
  }
}
