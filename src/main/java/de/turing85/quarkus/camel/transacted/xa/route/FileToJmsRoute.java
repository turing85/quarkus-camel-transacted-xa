package de.turing85.quarkus.camel.transacted.xa.route;

import jakarta.inject.Singleton;
import jakarta.jms.ConnectionFactory;

import de.turing85.quarkus.camel.transacted.xa.processor.SuspendRoutesProcessor;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.springframework.transaction.PlatformTransactionManager;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.file;
import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.jms;

@Singleton
@Getter(AccessLevel.PRIVATE)
public final class FileToJmsRoute extends BaseRoute {
  public static final String ROUTE_ID = "file-to-jms";

  public static final String IN_DIRECTORY_NAME = "in";
  public static final String OUT_QUEUE_NAME = "out";

  private final ConnectionFactory connectionFactory;

  FileToJmsRoute(SuspendRoutesProcessor suspendRoutesProcessor,
      PlatformTransactionManager platformTransactionManager,
      @SuppressWarnings("CdiInjectionPointsInspection") ConnectionFactory connectionFactory) {
    super(suspendRoutesProcessor, platformTransactionManager);
    this.connectionFactory = connectionFactory;
  }

  @Override
  public void configure() {
    super.configure();
    // @formatter:off
    from(file(IN_DIRECTORY_NAME).delete(true))
        .routeId(ROUTE_ID)
        .transacted()
        .convertBodyTo(String.class)
        .log(LoggingLevel.INFO,
            "file ${header.%s} has content: ${body}".formatted(Exchange.FILE_NAME))
        .to(jms(OUT_QUEUE_NAME)
            .connectionFactory(getConnectionFactory())
            .advanced()
                .transactionManager(getPlatformTransactionManager()));
    // @formatter:on
  }
}
