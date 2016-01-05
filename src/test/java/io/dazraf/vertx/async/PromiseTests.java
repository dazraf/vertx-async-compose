package io.dazraf.vertx.async;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.dazraf.vertx.async.promise.Promises;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(VertxUnitRunner.class)
public class PromiseTests {
  private static final Logger LOGGER = LoggerFactory.getLogger(PromiseTests.class);
  private static final String ADDRESS = "my.address";
  private static final String ID = "id";
  private static final String URI = "/api/hello";

  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

  @Test
  public void simpleEventBusSendTest(TestContext context) {
    final Async async = context.async();

    final EventBus eventBus = rule.vertx().eventBus();
    // a little ping service
    eventBus.consumer(ADDRESS).handler(message -> message.reply(message.body()));

    Promises
      .forEventBusMessageType(JsonObject.class)
      .invoke(callback -> eventBus.send(ADDRESS, new JsonObject().put(ID, 1), callback))

      .execute(ar -> {
        context.assertTrue(ar.succeeded());
        context.assertEquals(1, ar.result().body().getInteger(ID));
        async.complete();
      });
  }


  @Test
  public void simpleEventBusSendWithPeekTest(TestContext context) {
    final Async async = context.async();

    final EventBus eventBus = rule.vertx().eventBus();
    // a little ping service
    eventBus.consumer(ADDRESS).handler(message -> message.reply(message.body()));

    Promises
      .forEventBusMessageType(JsonObject.class).invoke(callback -> eventBus.send(ADDRESS, new JsonObject().put(ID, 1), callback))
      .peek(ar -> {
        context.assertTrue(ar.succeeded());
        context.assertEquals(1, ar.result().body().getInteger(ID));
        LOGGER.info("peeked successfully");
      })

      .execute(ar -> {
        context.assertTrue(ar.succeeded());
        context.assertEquals(1, ar.result().body().getInteger(ID));
        async.complete();
      });
  }


  @Test
  public void simpleEventBusExchangeWithFailureTest(TestContext context) {
    final Async async = context.async();

    final EventBus eventBus = rule.vertx().eventBus();
    // a little ping service
    eventBus.consumer(ADDRESS).handler(message -> message.fail(0, "will always fail"));

    Promises
      .forEventBusMessageType(JsonObject.class).invoke(callback -> eventBus.send(ADDRESS, new JsonObject().put(ID, 1), callback))
      .catchException(throwable -> {
        LOGGER.info("sending of message failed as expected", throwable);
        async.complete();
      })
      .execute(ar -> {
        context.fail("should never get here");
        async.complete();
      });
  }

  @Test
  public void multiEventBusTest(TestContext context) {
    final Async async = context.async();

    final AtomicInteger nextExpectedResult = new AtomicInteger(1);

    final EventBus eventBus = rule.vertx().eventBus();
    // a little ping service
    eventBus.consumer(ADDRESS).handler(message -> message.reply(message.body()));

    Promises
      .forEventBusMessageType(JsonObject.class)
      .invoke(callback -> eventBus.send(ADDRESS, new JsonObject().put(ID, 1), callback))
      .peek(ar -> context.assertEquals(ar.result().body().getInteger(ID), nextExpectedResult.getAndIncrement()))

      .thenOnEventBusType(JsonObject.class)
      .invoke(callback -> eventBus.send(ADDRESS, new JsonObject().put(ID, 2), callback))
      .peek(ar -> context.assertEquals(ar.result().body().getInteger(ID), nextExpectedResult.getAndIncrement()))

      .execute(ar -> async.complete());
  }

  @Test
  public void httpAPITest(TestContext context) throws IOException {

    Async async = context.async();

    // create a HTTP server on a free port
    int port = getFreePort();
    Router router = Router.router(rule.vertx());
    AtomicInteger nextInteger = new AtomicInteger(1);

    router.get(URI).handler(rc -> rc.response().end(new JsonObject().put(ID, nextInteger.getAndIncrement()).toString()));
    final HttpServer httpServer = rule.vertx().createHttpServer(new HttpServerOptions().setHost("localhost").setPort(port)).requestHandler(router::accept);

    // now create a HTTP client
    final HttpClient httpClient = rule.vertx().createHttpClient(new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port));

    Promises
      .forAsyncResultType(HttpServer.class).invoke(httpServer::listen) // start the web server
      .thenForCallbackResultType(HttpClientResponse.class).invoke(callback -> httpClient.getNow(URI, callback)) // invoke the web server
      .execute(ar -> async.complete());
  }

  @Test
  public void httpAPIExceptionTest(TestContext context) throws IOException {

    Async async = context.async();

    // pick a random free port
    int port = getFreePort();

    // now create a HTTP client
    final HttpClient httpClient = rule.vertx().createHttpClient(new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port));

    Promises
      .onHttpClientRequest(callback -> httpClient.get(URI, callback))
      .catchException(throwable -> {
        // this is what we expected
        context.assertEquals(ConnectException.class, throwable.getClass());
        async.complete();
      })
      .execute(ar -> context.fail("we should never get here"));
  }

  @Test
  public void testWithMongoDB(TestContext context) throws IOException {
    Async async = context.async();

    final MongodStarter starter = MongodStarter.getDefaultInstance();
    int port = getFreePort();
    final IMongodConfig mongodConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net(port, Network.localhostIsIPv6()))
      .build();

    final MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
    MongodProcess mongod = mongodExecutable.start();

    async.handler(ar -> {
      mongodExecutable.stop();
    });


    JsonObject mongoConfig = new JsonObject();
    mongoConfig.put("connection_string", "mongodb://localhost:" + port);
    mongoConfig.put("db_name", "db1");

    MongoClient client = MongoClient.createShared(rule.vertx(), mongoConfig);

    Promises.forAsyncResultType(String.class)
      .invoke(callback -> client.save("books", new JsonObject().put("title", "Finnegans Wake"), callback))
      .invoke(callback -> client.save("books", new JsonObject().put("title", "Nineteen Eighty-Four"), callback))
      .execute(ar -> async.complete());

  }

  private int getFreePort() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      return serverSocket.getLocalPort();
    }
  }
}
