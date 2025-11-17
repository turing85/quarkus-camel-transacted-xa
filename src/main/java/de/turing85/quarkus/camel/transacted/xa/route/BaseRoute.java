package de.turing85.quarkus.camel.transacted.xa.route;

import de.turing85.quarkus.camel.transacted.xa.processor.SuspendRoutesProcessor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.transaction.PlatformTransactionManager;

@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
abstract class BaseRoute extends RouteBuilder {
  @Getter(AccessLevel.PRIVATE)
  private final SuspendRoutesProcessor suspendRoutesProcessor;
  private final PlatformTransactionManager platformTransactionManager;

  @Override
  public void configure() {
    onException(Exception.class).process(getSuspendRoutesProcessor());
  }
}
