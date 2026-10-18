package com.portfolio.drivecare.notification;

import com.portfolio.drivecare.notification.NotificationDtos.NotificationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import com.portfolio.drivecare.observability.OperationLog;

@Service
public class NotificationService {

	private final NotificationRepository notifications;

	public NotificationService(NotificationRepository notifications) {
		this.notifications = notifications;
	}

	@Transactional(readOnly = true)
	public List<NotificationResponse> list(Long userId) {
		return notifications.findAllByUserIdAndMaintenanceItemActiveTrueOrderByCreatedAtDesc(userId).stream()
				.map(NotificationResponse::from)
				.toList();
	}

	@Transactional
	public NotificationResponse markRead(Long userId, Long notificationId) {
		Notification notification = notifications.findByIdAndUserIdAndMaintenanceItemActiveTrue(notificationId, userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
		notification.markRead();
		OperationLog.committed(OperationLog.Action.NOTIFICATION_READ, userId, notificationId);
		return NotificationResponse.from(notification);
	}

}
