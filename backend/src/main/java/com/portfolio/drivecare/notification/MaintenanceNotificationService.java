package com.portfolio.drivecare.notification;

import com.portfolio.drivecare.maintenance.MaintenanceItemRepository;
import com.portfolio.drivecare.maintenance.MaintenanceStatusCalculator;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceNotificationService {
	private final MaintenanceItemRepository items;
	private final NotificationRepository notifications;
	private final MaintenanceStatusCalculator calculator;
	public MaintenanceNotificationService(MaintenanceItemRepository items, NotificationRepository notifications,
			MaintenanceStatusCalculator calculator) {
		this.items = items;
		this.notifications = notifications;
		this.calculator = calculator;
	}

	/** Per-item transaction and lock also serialize concurrent scheduler instances and replacement writes. */
	@Transactional
	public boolean createForItem(Long itemId, LocalDate today) {
		var item = items.findActiveForUpdate(itemId).orElse(null);
		if (item == null) return false;
		var status = calculator.calculate(item, today);
		if (status.state() == MaintenanceStatusCalculator.State.GOOD) return false;
		NotificationType type = status.state() == MaintenanceStatusCalculator.State.OVERDUE
				? NotificationType.OVERDUE : NotificationType.DUE_SOON;
		String key = item.getId() + ":" + type + ":" + status.nextOdometerKm() + ":" + status.nextDate();
		if (notifications.existsByDedupeKey(key)) return false;
		String title = type == NotificationType.OVERDUE ? "소모품 교체주기 초과" : "소모품 교체 임박";
		notifications.save(new Notification(item.getVehicle().getOwner(), item, type, title,
				NotificationMessage.build(item, status, today), key));
		return true;
	}
}
