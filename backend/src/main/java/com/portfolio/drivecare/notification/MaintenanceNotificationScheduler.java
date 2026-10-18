package com.portfolio.drivecare.notification;

import com.portfolio.drivecare.maintenance.MaintenanceItemRepository;
import com.portfolio.drivecare.observability.ExceptionDiagnostics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class MaintenanceNotificationScheduler {
	private static final Logger log = LoggerFactory.getLogger(MaintenanceNotificationScheduler.class);
	private final MaintenanceItemRepository items;
	private final MaintenanceNotificationService service;
	private final Clock clock;
	private final int batchSize;

	public MaintenanceNotificationScheduler(MaintenanceItemRepository items, MaintenanceNotificationService service,
			Clock clock, @Value("${app.notifications.batch-size:100}") int batchSize) {
		if (batchSize < 1 || batchSize > 1000) throw new IllegalArgumentException("Notification batch size must be 1..1000");
		this.items = items;
		this.service = service;
		this.clock = clock;
		this.batchSize = batchSize;
	}

	@Scheduled(cron = "${app.notifications.cron}", zone = "${app.notifications.zone}")
	public void createDueNotifications() {
		long started = System.nanoTime();
		int scanned = 0, created = 0, failed = 0;
		try (var ignored = MDC.putCloseable("jobId", UUID.randomUUID().toString())) {
			log.info("event=notification_job_started batchSize={}", batchSize);
			try {
				LocalDate today = LocalDate.now(clock);
				long cursor = 0, upperId = items.findMaxActiveId();
				while (cursor < upperId) {
					var ids = items.findActiveIds(cursor, upperId, PageRequest.of(0, batchSize));
					if (ids.isEmpty()) break;
					for (Long id : ids) {
						scanned++;
						try { if (service.createForItem(id, today)) created++; }
						catch (Exception exception) {
							failed++;
							log.error("event=notification_item_failed itemId={} diagnostic={}", id, ExceptionDiagnostics.describe(exception));
						}
					}
					cursor = ids.get(ids.size() - 1);
				}
			} catch (Exception exception) {
				log.error("event=notification_job_aborted scanned={} created={} diagnostic={}",
						scanned, created, ExceptionDiagnostics.describe(exception));
				return;
			}
			log.info("event=notification_job_completed scanned={} created={} skipped={} failed={} durationMs={}",
					scanned, created, scanned - created - failed, failed, (System.nanoTime() - started) / 1_000_000);
		}
	}
}
