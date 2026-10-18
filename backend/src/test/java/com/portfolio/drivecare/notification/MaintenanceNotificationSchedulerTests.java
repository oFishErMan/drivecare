package com.portfolio.drivecare.notification;

import com.portfolio.drivecare.maintenance.MaintenanceItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MaintenanceNotificationSchedulerTests {
	@Test
	void oneFailedItemDoesNotStopLaterItemsOrPages() {
		var items = mock(MaintenanceItemRepository.class);
		var service = mock(MaintenanceNotificationService.class);
		Clock clock = Clock.fixed(Instant.parse("2026-10-09T16:00:00Z"), ZoneId.of("Asia/Seoul"));
		LocalDate today = LocalDate.of(2026, 10, 10);
		when(items.findMaxActiveId()).thenReturn(3L);
		when(items.findActiveIds(eq(0L), eq(3L), any(Pageable.class))).thenReturn(List.of(1L, 2L));
		when(items.findActiveIds(eq(2L), eq(3L), any(Pageable.class))).thenReturn(List.of(3L));
		when(service.createForItem(1L, today)).thenThrow(new IllegalStateException("simulated"));
		when(service.createForItem(2L, today)).thenReturn(true);
		new MaintenanceNotificationScheduler(items, service, clock, 2).createDueNotifications();
		verify(service).createForItem(1L, today);
		verify(service).createForItem(2L, today);
		verify(service).createForItem(3L, today);
	}
}
