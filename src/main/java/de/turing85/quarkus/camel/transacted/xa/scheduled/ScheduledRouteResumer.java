package de.turing85.quarkus.camel.transacted.xa.scheduled;

import java.util.concurrent.ExecutorService;

import jakarta.inject.Singleton;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.camel.CamelContext;

@Singleton
@Getter(AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ScheduledRouteResumer {
  private final CamelContext camelContext;
  private final ExecutorService executor;

  @Scheduled(cron = "0 * * * * ?")
  void resume() {
    Log.debug("Resume?");
    if (!getCamelContext().isStarted() && !getCamelContext().isStarting()) {
      Log.debug("Resume!");
      getExecutor().execute(camelContext::resume);
    } else {
      Log.debug("No Resume...");
    }
  }
}
