package de.turing85.quarkus.camel.transacted.xa.config;

import jakarta.enterprise.inject.Produces;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

class PlatformTransactionManagerProducer {
  @Produces
  PlatformTransactionManager platformTransactionManager(UserTransaction userTransaction,
      @SuppressWarnings("CdiInjectionPointsInspection") TransactionManager transactionManager) {
    return new JtaTransactionManager(userTransaction, transactionManager);
  }
}
