package io.dazraf.vertx.async.promise;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

import java.util.function.Consumer;

import static io.dazraf.vertx.async.promise.Promises.*;

public interface Promise<Result> {
  void eval(Handler<AsyncResult<Result>> handler);

  /**
   * Provides an opportunity for the client to inspect the response value asyncResult of a promise before it is dispatched
   * to the next promise in the chain
   * @param peekFn a callback function of type {@link Consumer}
   * @return this promise chained with the peek function
   */
  default Promise<Result> peek(Consumer<AsyncResult<Result>> peekFn) {
    return handler -> this.eval(thisAsyncResult -> peekAndChain(thisAsyncResult, peekFn, handler));
  }

  /**
   * Chain the next async function returning a promise.
   * In this case, we chain a standard Vert.x function of the form {@code fn(parameter1, parameter2, Handler<AsyncResult<Message<Result>>}
   *
   * An example of this type of function is {@link io.vertx.core.eventbus.EventBus#send(String, Object, Handler)}
   * Other function types will require additional (default) implementations in this interface
   *
   * @param resultType  Required to bind the result type (gaps in Java 8 generics)
   * @param fn          The async function to be invoked
   * @param p1          The first parameter to the async function
   * @param p2          The second parameter to the async function
   * @param <Result2>   The type of result being returned from the exeuction of this new function
   * @param <P1>        The type of the first parameter to the async function
   * @param <P2>        The type of the second parameter to the async function
   * @return            A promise representing the chain up to this object, followed by the new function 'fn'
   */
  default <Result2, P1, P2> Promise<Message<Result2>> then(Class<Result2> resultType, AsyncFunctions.Fn2<Message<Result2>, P1, P2> fn, P1 p1, P2 p2) {
    return result2Handler -> this.eval(thisAsyncResult -> handleErrorOrChain(fn, p1, p2, result2Handler, thisAsyncResult));
  }

  /**
   * Execute all promises in parallel after this promise has successfully completed.
   * The callback from the promise will be successful() if all promises completely successfully; or failed()
   * if any promise failed. In the case of a failure, the cause will record the failure cause of the first promise to fail
   *
   * @param args zero or more promises
   * @return a Promise representing the parallel exeuction of all promises
   */
  default Promise<Object> thenAll(Promise ... args) {
    return allHandler -> this.eval(thisAsyncResult -> handleErrorOrChainAll(allHandler, thisAsyncResult, args));
  }
}
