package com.portfolio.drivecare.notification;

import java.time.Instant;

public final class NotificationDtos {
	private NotificationDtos() { }

	public record NotificationResponse(Long id, NotificationType type, String title, String message,
			Instant readAt, Instant createdAt, Long maintenanceItemId) {
		static NotificationResponse from(Notification notification) {
			return new NotificationResponse(notification.getId(), notification.getNotificationType(),
					notification.getTitle(), notification.getMessage(), notification.getReadAt(), notification.getCreatedAt(),
					notification.getSourceItemId());
		}
	}
}
