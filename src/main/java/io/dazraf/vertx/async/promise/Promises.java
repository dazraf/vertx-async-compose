package io.dazraf.vertx.async.promise;

import io.dazraf.vertx.async.promise.builders.HttpClientRequestPromiseBuilder;
import io.dazraf.vertx.async.promise.builders.PromiseBuilder;
import io.dazraf.vertx.async.promise.builders.RawPromiseBuilder;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

import java.util.function.Function;

public class Promises {

  public static <Result> PromiseBuilder<Message<Result>> forEventBusMessageType(Class<Result> messageType) {
    return implementation -> implementation::accept;
  }

  public static <Result> PromiseBuilder<Result> forAsyncResultType(Class<Result> resultType) {
    return implementation -> implementation::accept;
  }

  public static <Result> RawPromiseBuilder<Result> forCallbackType(Class<Result> callbackType) {
    return new RawPromiseBuilder<>();
  }

  public static Promise<HttpClientResponse> onHttpClientRequest(Function<Handler<HttpClientResponse>, HttpClientRequest> implementation) {
    return new HttpClientRequestPromiseBuilder().invoke(implementation);
  }
}
