package io.dazraf.vertx.async.promise.builders;

import io.vertx.core.AsyncResult;

class AsyncResultWrapper {
  private AsyncResultWrapper() {}

  public static <T> AsyncResult<T> success(T result) {
    return new AsyncResult<T>() {
      @Override
      public T result() {
        return result;
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

  public static <T> AsyncResult<T> failed(Throwable cause) {
    return new AsyncResult<T>() {
      @Override
      public T result() {
        return null;
      }

      @Override
      public Throwable cause() {
        return cause;
      }

      @Override
      public boolean succeeded() {
        return false;
      }

      @Override
      public boolean failed() {
        return true;
      }
    };
  }
}
