package com.portfolio.drivecare.notification;

import com.portfolio.drivecare.maintenance.MaintenanceItem;
import com.portfolio.drivecare.maintenance.MaintenanceStatusCalculator;
import java.time.LocalDate;

final class NotificationMessage {
	private NotificationMessage() { }
	static String build(MaintenanceItem item, MaintenanceStatusCalculator.Status status, LocalDate today) {
		String product = item.getProductName();
		if (product == null || product.isBlank()) product = switch (item.getCategory()) {
			case ENGINE_OIL -> "엔진오일"; case OIL_FILTER -> "오일 필터";
			case AIR_FILTER -> "에어 필터"; case CABIN_FILTER -> "에어컨 필터";
			case BRAKE_FLUID -> "브레이크액"; case BRAKE_PAD -> "브레이크 패드";
			case COOLANT -> "냉각수"; case TRANSMISSION_FLUID -> "변속기 오일";
			case TIRE -> "타이어"; case WIPER -> "와이퍼";
			case BATTERY -> "배터리"; case OTHER -> "기타 소모품";
		};
		// An expired date takes precedence even when there is still mileage remaining.
		if (status.nextDate() != null && !status.nextDate().isAfter(today)) {
			return product + " 기간 기준 교체일(" + status.nextDate() + ")에 도달했습니다.";
		}
		if (status.remainingKm() != null && status.remainingKm() < 0) {
			return product + " 교체주기를 " + Math.abs((long) status.remainingKm()) + "km 초과했습니다.";
		}
		if (status.remainingKm() != null && status.remainingKm() == 0) return product + " 교체 시점에 도달했습니다.";
		if (status.nextDate() != null && !status.nextDate().isAfter(today.plusDays(MaintenanceStatusCalculator.DUE_SOON_DAYS))) {
			return product + " 기간 기준 교체 예정일은 " + status.nextDate() + "입니다.";
		}
		return product + " 교체까지 " + status.remainingKm() + "km 남았습니다.";
	}
}
