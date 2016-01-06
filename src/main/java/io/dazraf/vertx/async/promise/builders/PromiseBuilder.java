package io.dazraf.vertx.async.promise.builders;

import io.dazraf.vertx.async.promise.Promise;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.function.Consumer;

@FunctionalInterface
public interface PromiseBuilder<Result> {
  Promise<Result> then(Consumer<Handler<AsyncResult<Result>>> implementation);
}
