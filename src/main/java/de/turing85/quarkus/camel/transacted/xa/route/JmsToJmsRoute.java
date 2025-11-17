package de.turing85.quarkus.camel.transacted.xa.route;

import jakarta.inject.Singleton;
import jakarta.jms.ConnectionFactory;

import de.turing85.quarkus.camel.transacted.xa.processor.SuspendRoutesProcessor;
import io.quarkus.artemis.core.runtime.ArtemisUtil;
import io.smallrye.common.annotation.Identifier;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.camel.LoggingLevel;
import org.springframework.transaction.PlatformTransactionManager;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.jms;

@Singleton
@Getter(AccessLevel.PRIVATE)
public final class JmsToJmsRoute extends BaseRoute {
  public static final String ROUTE_ID = "jms-to-jms";

  public static final String IN_QUEUE_NAME = "in";
  public static final String OUT_QUEUE_NAME = "out";

  private final ConnectionFactory connectionFactory;
  private final ConnectionFactory secondConnectionFactory;
  private final PlatformTransactionManager transactionManager;

  JmsToJmsRoute(SuspendRoutesProcessor suspendRoutesProcessor,
      PlatformTransactionManager transactionManager,

      @Identifier(ArtemisUtil.DEFAULT_CONFIG_NAME)
      @SuppressWarnings("CdiInjectionPointsInspection") ConnectionFactory connectionFactory,

      @Identifier("second")
      @SuppressWarnings("CdiInjectionPointsInspection") ConnectionFactory secondConnectionFactory) {
    super(suspendRoutesProcessor, transactionManager);
    this.connectionFactory = connectionFactory;
    this.secondConnectionFactory = secondConnectionFactory;
    this.transactionManager = transactionManager;
  }

  @Override
  public void configure() {
    super.configure();

    // @formatter:off
    from(
        jms(IN_QUEUE_NAME)
            .connectionFactory(getConnectionFactory())
            .advanced()
                .transactionManager(getTransactionManager()))
        .routeId(ROUTE_ID)
        .transacted()
        .log(LoggingLevel.INFO, "Received message has content: ${body}")
        .to(jms(OUT_QUEUE_NAME)
            .connectionFactory(getSecondConnectionFactory())
            .advanced()
                .transactionManager(getTransactionManager()));
    // @formatter:on
  }
}
