package com.portfolio.drivecare.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;

class OperationLogTests {
	@Test
	void onlyCommittedWorkProducesSuccessLogs() {
		var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(OperationLog.class);
		var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
		appender.start(); logger.addAppender(appender);
		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		try {
			OperationLog.committed(OperationLog.Action.MAINTENANCE_REPLACED, 1L, 2L);
			assertTrue(appender.list.isEmpty());
			TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
			assertEquals(1, appender.list.size());
			assertTrue(appender.list.get(0).getFormattedMessage().contains("MAINTENANCE_REPLACED"));
		} finally {
			TransactionSynchronizationManager.clear();
			logger.detachAppender(appender); appender.stop();
		}
		assertThrows(IllegalStateException.class, () -> OperationLog.committed(OperationLog.Action.VEHICLE_CREATED, 1L, 2L));
	}
}
