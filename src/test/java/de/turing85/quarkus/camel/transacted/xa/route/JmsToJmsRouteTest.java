package de.turing85.quarkus.camel.transacted.xa.route;

import java.util.UUID;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.ws.rs.core.Response;

import de.turing85.quarkus.camel.transacted.xa.route.helper.JmsHelper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static de.turing85.quarkus.camel.transacted.xa.route.helper.HasBodyMatcher.hasBody;

@QuarkusTest
class JmsToJmsRouteTest extends BaseTest {
  JmsToJmsRouteTest(JmsHelper jmsHelper) {
    super(jmsHelper);
  }

  @Test
  void testOk() {
    // given
    final String messageContent = UUID.randomUUID().toString();
    try (final JMSContext jmsContext = getConnectionFactory().createContext();
        final JMSContext secondJmsContext = getSecondConnectionFactory().createContext();
        final JMSConsumer secondConsumer = secondJmsContext
            .createConsumer(secondJmsContext.createQueue(JmsToJmsRoute.OUT_QUEUE_NAME))) {

      // when
      jmsContext.createProducer().send(jmsContext.createQueue(JmsToJmsRoute.IN_QUEUE_NAME),
          messageContent);

      // then
      assertThatMessageOn(secondConsumer, hasBody(messageContent));
      assertNoMessageOn(jmsContext, JmsToJmsRoute.IN_QUEUE_NAME);
    }
  }

  @Test
  void testRollback() throws Exception {
    // given
    final String messageContent = UUID.randomUUID().toString();
    final String throwerId = adviceWeaveAddLastThrow(JmsToJmsRoute.ROUTE_ID,
        new RuntimeException("Artificial exception to test rollback"));
    awaitThatHealthReports(Response.Status.OK);
    try (final JMSContext jmsContext = getConnectionFactory().createContext();
        final JMSContext secondJmsContext = getSecondConnectionFactory().createContext();
        final JMSConsumer secondConsumer = secondJmsContext
            .createConsumer(secondJmsContext.createQueue(JmsToJmsRoute.OUT_QUEUE_NAME))) {

      // when
      jmsContext.createProducer().send(jmsContext.createQueue(JmsToJmsRoute.IN_QUEUE_NAME),
          messageContent);

      // then
      awaitThatHealthReports(Response.Status.SERVICE_UNAVAILABLE);
      assertNoMessageOn(secondConsumer);
      assertThatMessageOn(jmsContext, JmsToJmsRoute.IN_QUEUE_NAME, hasBody(messageContent));
    } finally {
      adviceRemove(JmsToJmsRoute.ROUTE_ID, throwerId);
    }
  }
}
