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

  public static <Result> PromiseBuilder<Message<Result>> eventBusMessage() {
    return implementation -> implementation::accept;
  }

  public static <Result> PromiseBuilder<Result> asyncResult() {
    return implementation -> implementation::accept;
  }

  public static <Result> RawPromiseBuilder<Result> callback() {
    return new RawPromiseBuilder<>();
  }

  public static Promise<HttpClientResponse> httpClientRequest(Function<Handler<HttpClientResponse>, HttpClientRequest> implementation) {
    return new HttpClientRequestPromiseBuilder().then(implementation);
  }
}
