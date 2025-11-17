package de.turing85.quarkus.camel.transacted.xa.processor;

import java.util.concurrent.ExecutorService;

import jakarta.inject.Singleton;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

@Singleton
@Getter(AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class SuspendRoutesProcessor implements Processor {
  private final CamelContext camelContext;
  private final ExecutorService executor;

  @Override
  public void process(Exchange unused) {
    if (!getCamelContext().isSuspended() && !getCamelContext().isSuspending()) {
      getExecutor().execute(getCamelContext()::suspend);
    }
  }
}
