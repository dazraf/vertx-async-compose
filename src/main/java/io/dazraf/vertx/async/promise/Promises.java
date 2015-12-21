package io.dazraf.vertx.async.promise;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.stream.Stream.of;

/**
 * A simple, extensible Promise API
 *
 */
public class Promises {

  public static <R, P1, P2> Promise<Message<R>> create(Class<R> returnType, AsyncFunctions.Fn2<Message<R>, P1, P2> fn, P1 p1, P2 p2) {
    return handler -> fn.call(p1, p2, handler);
  }

  /**
   * Execute all promises in parallel. The callback from the promise will be successful() if all promises completely successfully; or failed()
   * if any promise failed. In the case of a failure, the cause will record the failure cause of the first promise to fail
   *
   * @param args zero or more promises
   * @return a Promise representing the parallel exeuction of all promises
   */
  @SuppressWarnings("unchecked")
  public static Promise<Object> all(Promise ... args) {
    return handler -> processAllPromises(handler, args);
  }


  // ------------- private / internal implementation ------------------

  static <Result> void peekAndChain(AsyncResult<Result> thisAsyncResult, Consumer<AsyncResult<Result>> peekFn, Handler<AsyncResult<Result>> handler) {
    peekFn.accept(thisAsyncResult);
    handler.handle(thisAsyncResult);
  }

  @SuppressWarnings("unchecked") // "Oh Java, your generics are so inferior"
  static <Result, Result2, P1, P2> void handleErrorOrChain(AsyncFunctions.Fn2<Message<Result2>, P1, P2> fn, P1 p1, P2 p2, Handler<AsyncResult<Message<Result2>>> result2Handler, AsyncResult<Result> thisAsyncResult) {
    if (thisAsyncResult.failed()) {
      result2Handler.handle((AsyncResult<Message<Result2>>) thisAsyncResult);
    } else {
      // call the next function in the chain
      fn.call(p1, p2, result2Handler);
    }
  }

  @SuppressWarnings("unchecked") // "Oh Java, your generics are so inferior"
  static <Result> void handleErrorOrChainAll(Handler<AsyncResult<Object>> allHandler, AsyncResult<Result> thisAsyncResult, Promise[] args) {
    if (thisAsyncResult.failed()) {
      allHandler.handle((AsyncResult<Object>)thisAsyncResult);
    } else {
      all(args).eval(allHandler);
    }
  }

  private static void processPromise(Handler<AsyncResult<Object>> handler,
                             Promise<Object> promise,
                             AtomicReference<AsyncResult<Object>> failedResult,
                             AtomicInteger count) {
    promise.eval(asyncResult -> handlePromiseResult(handler, failedResult, count, asyncResult));
  }

  private static void handlePromiseResult(Handler<AsyncResult<Object>> handler, AtomicReference<AsyncResult<Object>> failedResult, AtomicInteger count, AsyncResult<Object> asyncResult) {
    if (asyncResult.failed()) { // if this promise failed and it's the first promise to fail, then record the result
      failedResult.compareAndSet(null, asyncResult);
    }

    if (count.decrementAndGet() == 0) { // if we've done all promises
      handleCompletionOfAllPromises(handler, failedResult);
    }
  }

  private static void handleCompletionOfAllPromises(Handler<AsyncResult<Object>> handler, AtomicReference<AsyncResult<Object>> failedResult) {
    if (failedResult.get() != null) {
      // failed case
      handler.handle(failedResult.get());
    } else {
      handler.handle(createSuccessfulAsyncResult(null));
    }
  }

  private static void processAllPromises(Handler<AsyncResult<Object>> handler, Promise[] args) {
    if (args.length > 0) {
      final AtomicReference<AsyncResult<Object>> failedResult = new AtomicReference<>();
      final AtomicInteger count = new AtomicInteger(args.length); // counts down as each promise completes
      // process the promises as a parallel Stream
      of(args).parallel().forEach(promise -> processPromise(handler, promise, failedResult, count));
    } else {
      handler.handle(createSuccessfulAsyncResult(null));
    }
  }


  // this is used to signify a successful result from an 'all' execution.
  // at present, the spec is to return a successful result but the result value is not bound
  private static <T> AsyncResult<T> createSuccessfulAsyncResult(T value) {
    return new AsyncResult<T>() {
      @Override
      public T result() {
        return value;
      }

      @Override
      public Throwable cause() {
        return null;
      }

      @Override
      public boolean succeeded() {
        return true;
      }

      @Override
      public boolean failed() {
        return false;
      }
    };
  }
}
