package io.dazraf.vertx.async.promise.builders;

import io.dazraf.vertx.async.promise.Promise;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.function.Consumer;

@FunctionalInterface
public interface PromiseBuilder<Result> {
  Promise<Result> invoke(Consumer<Handler<AsyncResult<Result>>> implementation);
}
