package de.turing85.quarkus.camel.transacted.xa.route;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.ws.rs.core.Response;

import de.turing85.quarkus.camel.transacted.xa.route.helper.JmsHelper;
import io.quarkus.test.junit.QuarkusTest;
import lombok.AccessLevel;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import static de.turing85.quarkus.camel.transacted.xa.route.helper.HasBodyMatcher.hasBody;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
@Getter(AccessLevel.PRIVATE)
class FileToJmsRouteTest extends BaseTest {
  FileToJmsRouteTest(JmsHelper jmsHelper) {
    super(jmsHelper);
  }

  @Test
  void testOk() throws IOException {
    // given
    final Path file = getInDir().resolve("input-%s.txt".formatted(UUID.randomUUID()));
    final String fileContent = UUID.randomUUID().toString();
    try (final JMSContext jmsContext = getConnectionFactory().createContext();
        final JMSConsumer consumer =
            jmsContext.createConsumer(jmsContext.createQueue(FileToJmsRoute.OUT_QUEUE_NAME))) {

      // when
      Files.writeString(file, fileContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

      // then
      assertThatMessageOn(consumer, hasBody(fileContent));
      assertThat(Files.exists(file), is(false));
    }
  }

  @Test
  void testRollback() throws Exception {
    // given
    final Path file = getInDir().resolve("input-%s.txt".formatted(UUID.randomUUID()));
    final String fileContent = UUID.randomUUID().toString();
    final String throwerId = adviceWeaveAddLastThrow(FileToJmsRoute.ROUTE_ID,
        new RuntimeException("Artificial exception to test rollback"));
    awaitThatHealthReports(Response.Status.OK);
    try (final JMSContext jmsContext = getConnectionFactory().createContext();
        final JMSConsumer consumer =
            jmsContext.createConsumer(jmsContext.createQueue(FileToJmsRoute.OUT_QUEUE_NAME))) {

      // when
      Files.writeString(file, fileContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

      // then
      awaitThatHealthReports(Response.Status.SERVICE_UNAVAILABLE);
      assertNoMessageOn(consumer);
      assertThat(Files.exists(file), is(true));
      assertThat(Files.readString(file), is(fileContent));
    } finally {
      adviceRemove(FileToJmsRoute.ROUTE_ID, throwerId);
    }
  }
}
