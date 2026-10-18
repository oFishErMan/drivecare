package com.portfolio.drivecare.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Logs committed business changes without recording request bodies or product URLs. */
public final class OperationLog {
	private static final Logger log = LoggerFactory.getLogger(OperationLog.class);
	private OperationLog() { }

	public enum Action {
		USER_REGISTERED, VEHICLE_CREATED, ODOMETER_UPDATED,
		MAINTENANCE_CREATED, PURCHASE_LINK_UPDATED, MAINTENANCE_REPLACED, MAINTENANCE_DELETED, NOTIFICATION_READ
	}

	public static void committed(Action action, Long userId, Long entityId) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new IllegalStateException("Operation logging requires a transaction");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override public void afterCommit() {
				log.info("event=business_change action={} userId={} entityId={}", action, userId, entityId);
			}
		});
	}
}
