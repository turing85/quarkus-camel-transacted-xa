package de.turing85.quarkus.camel.transacted.xa.route.helper;

import java.time.Duration;

import jakarta.inject.Singleton;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;

import de.turing85.quarkus.camel.transacted.xa.route.FileToJmsRoute;
import de.turing85.quarkus.camel.transacted.xa.route.JmsToJmsRoute;
import io.quarkus.artemis.core.runtime.ArtemisUtil;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Identifier;
import lombok.Getter;

@Singleton
@Getter
public final class JmsHelper {
  private final ConnectionFactory connectionFactory;
  private final ConnectionFactory secondConnectionFactory;

  public JmsHelper(
      @Identifier(ArtemisUtil.DEFAULT_CONFIG_NAME)
      @SuppressWarnings("CdiInjectionPointsInspection") ConnectionFactory connectionFactory,
      @Identifier("second")
      @SuppressWarnings("CdiInjectionPointsInspection") ConnectionFactory secondConnectionFactory) {
    this.connectionFactory = connectionFactory;
    this.secondConnectionFactory = secondConnectionFactory;
  }

  public void purgeQueues() {
    purgeQueue(JmsToJmsRoute.IN_QUEUE_NAME);
    purgeQueue(FileToJmsRoute.OUT_QUEUE_NAME);
    purgeQueue(JmsToJmsRoute.OUT_QUEUE_NAME);
  }

  private void purgeQueue(String queueName) {
    try (final JMSContext jmsContext = getConnectionFactory().createContext();
        final JMSConsumer consumer = jmsContext.createConsumer(jmsContext.createQueue(queueName))) {
      int counter = 0;
      while (consumer.receive(Duration.ofMillis(100).toMillis()) != null) {
        ++counter;
      }
      Log.debugf("%d messages removed from queue %s", counter, queueName);
    }
  }
}
