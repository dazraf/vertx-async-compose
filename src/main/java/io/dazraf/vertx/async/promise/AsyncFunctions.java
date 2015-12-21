package io.dazraf.vertx.async.promise;


import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface AsyncFunctions {
  @FunctionalInterface
  interface Fn2<R, P1, P2> {
    void call(P1 p1, P2 p2, Handler<AsyncResult<R>> callback);
  }

  // others can be added here
}
