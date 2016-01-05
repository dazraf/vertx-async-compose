package io.dazraf.vertx.async.promise;

import io.dazraf.vertx.async.promise.builders.ChainedHttpClientRequestPromiseBuilder;
import io.dazraf.vertx.async.promise.builders.ChainedPromiseBuilder;
import io.dazraf.vertx.async.promise.builders.ChainedRawPromiseBuilder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@FunctionalInterface
public interface Promise<Result> {

  void execute(Handler<AsyncResult<Result>> callback);

  default void execute() {
    this.execute(asyncResult-> {});
  }

  default Promise<Result> catchException(Consumer<Throwable> errorHandler) {
    return callback -> this.execute(asyncResult -> {
      if (asyncResult.failed()) {
        errorHandler.accept(asyncResult.cause());
      } else {
        callback.handle(asyncResult);
      }
    });
  }

  default Promise<Result> invoke(Consumer<Handler<AsyncResult<Result>>> implementation) {
    return callback -> {
      this.execute(asyncResult -> {
        if (asyncResult.succeeded()) {
          implementation.accept(callback);
        } else {
          callback.handle(asyncResult);
        }
      });
    };
  }

  default Promise<Result> invoke(BiConsumer<AsyncResult<Result>, Handler<AsyncResult<Result>>> implementation) {
    return callback -> {
      this.execute(asyncResult -> {
        if (asyncResult.succeeded()) {
          implementation.accept(asyncResult, callback);
        } else {
          callback.handle(asyncResult);
        }
      });
    };
  }

  default Promise<Result> peek(Consumer<AsyncResult<Result>> peekHandler) {
    return callback -> {
      this.execute(asyncResult -> {
        peekHandler.accept(asyncResult);
        callback.handle(asyncResult);
      });
    };
  }

  default <MessageType> ChainedPromiseBuilder<Result, Message<MessageType>> thenOnEventBusType(Class<MessageType> messageType) {
    return new ChainedPromiseBuilder<>(this);
  }

  default <NextResult> ChainedPromiseBuilder<Result, NextResult> thenForAsyncResultType(Class<NextResult> nextResultType) {
    return new ChainedPromiseBuilder<>(this);
  }

  default <NextResult> ChainedRawPromiseBuilder<Result, NextResult> thenForCallbackResultType(Class<NextResult> callBackResultType) {
    return new ChainedRawPromiseBuilder<>(this);
  }

  default ChainedHttpClientRequestPromiseBuilder<Result> thenForHttpClientRequest() {
    return new ChainedHttpClientRequestPromiseBuilder<>(this);
  }
}
