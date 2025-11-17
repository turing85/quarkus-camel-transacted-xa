package de.turing85.quarkus.camel.transacted.xa.route;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.ws.rs.core.Response;

import de.turing85.quarkus.camel.transacted.xa.route.helper.JmsHelper;
import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.logging.Log;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.restassured.RestAssured;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@WithTestResource(ArtemisTestResource.class)
@WithTestResource(value = ArtemisTestResource.class,
    initArgs = {@ResourceArg(name = "configurationName", value = "second")})
@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
abstract class BaseTest extends CamelQuarkusTestSupport {
  private static final Path IN_DIR = Path.of(FileToJmsRoute.IN_DIRECTORY_NAME);

  @Delegate
  private final JmsHelper jmsHelper;

  @BeforeEach
  void setup() throws Exception {
    Log.debug("executing: setup");
    try {
      stopAllRoutes();
      suspendContext();
      purgeQueues();
      removeAllFilesFromIn();
      resumeContext();
      startAllRoutes();
      awaitThatHealthReports(Response.Status.OK);
    } finally {
      Log.debug("executed: setup");
    }
  }

  protected Path getInDir() {
    return IN_DIR;
  }

  private void stopAllRoutes() throws Exception {
    context.getRouteController().reloadAllRoutes();
    // @formatter:off
    Awaitility.await()
        .atMost(Duration.ofSeconds(1))
        .until(() -> !context.getRouteController().isReloadingRoutes());
    // @formatter:on
  }

  private void suspendContext() {
    if (!context.isSuspended() && !context.isSuspending()) {
      context.suspend();
      // @formatter:off
      Awaitility.await()
          .atMost(Duration.ofSeconds(1))
          .until(context::isSuspended);
      // @formatter:on
    }
  }

  private static void removeAllFilesFromIn() throws IOException {
    try (final Stream<Path> paths = Files.walk(IN_DIR)) {
      // @formatter:off
      paths
          .filter(Files::isRegularFile)
          .forEach(path -> {
            try {
              Files.delete(path);
              Log.debugf("Deleted file %s", path);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
      // @formatter:on
    }
  }

  private void resumeContext() {
    if (!context.isStarted() && !context.isStarting()) {
      context.resume();
      // @formatter:off
      Awaitility.await()
          .atMost(Duration.ofSeconds(1))
          .until(context::isStarted);
      // @formatter:on
    }
  }

  private void startAllRoutes() throws Exception {
    context.getRouteController().startAllRoutes();
    // @formatter:off
    Awaitility.await()
        .atMost(Duration.ofSeconds(1))
        .until(() -> !context.getRouteController().isStartingRoutes());
    // @formatter:on
  }

  protected static void awaitThatHealthReports(Response.Status status) {
    // @formatter:off
    Awaitility.await()
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofSeconds(1))
        .untilAsserted(() -> RestAssured
            .when().get("/q/health")
            .then().statusCode(is(status.getStatusCode())));
    // @formatter:on
  }

  protected String adviceWeaveAddLastThrow(String routeId, Exception exception) throws Exception {
    final String id = UUID.randomUUID().toString();
    // @formatter:off
    AdviceWith.adviceWith(
        context(),
        routeId,
        advice -> advice.weaveAddLast()
            .throwException(exception)
            .id(id));
    // @formatter:on
    return id;
  }

  protected void adviceRemove(String routeId, String id) throws Exception {
    // @formatter:off
    AdviceWith.adviceWith(
        context(),
        routeId,
        advice -> advice.weaveById(id).remove());
    // @formatter:on
  }

  protected static void assertThatMessageOn(JMSContext jmsContext, String queue,
      Matcher<? super Message> matcher) {
    try (final JMSConsumer consumer = jmsContext.createConsumer(jmsContext.createQueue(queue))) {
      assertThatMessageOn(consumer, matcher);
    }
  }

  protected static void assertThatMessageOn(JMSConsumer consumer,
      Matcher<? super Message> matcher) {
    final Message message = consumer.receive(Duration.ofSeconds(5).toMillis());
    assertThat(message, matcher);
  }

  protected static void assertNoMessageOn(JMSContext jmsContext, String queue) {
    try (final JMSConsumer consumer = jmsContext.createConsumer(jmsContext.createQueue(queue))) {
      assertNoMessageOn(consumer);
    }
  }

  protected static void assertNoMessageOn(JMSConsumer consumer) {
    assertThat(consumer.receive(Duration.ofMillis(100).toMillis()), is(nullValue()));
  }
}
