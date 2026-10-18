package com.portfolio.drivecare.notification;

import com.portfolio.drivecare.maintenance.MaintenanceCategory;
import com.portfolio.drivecare.maintenance.MaintenanceItem;
import com.portfolio.drivecare.maintenance.MaintenanceStatusCalculator;
import com.portfolio.drivecare.user.AppUser;
import com.portfolio.drivecare.vehicle.Vehicle;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageTests {
	private final LocalDate today = LocalDate.of(2026, 10, 11);
	private final MaintenanceStatusCalculator calculator = new MaintenanceStatusCalculator();

	@Test
	void expiredDateTakesPrecedenceOverRemainingMileage() {
		var item = item(10000, 20000, today.minusMonths(12), 12);
		assertThat(message(item)).contains("기간 기준 교체일", "도달했습니다").doesNotContain("남았습니다");
	}

	@Test
	void zeroMileageMeansReachedRatherThanZeroExceeded() {
		var item = item(10000, 10000, today.minusMonths(1), null);
		assertThat(message(item)).isEqualTo("엔진오일 교체 시점에 도달했습니다.");
	}

	@Test
	void negativeMileageIsDisplayedAsPositiveExcess() {
		var item = item(15100, 10000, today.minusMonths(1), null);
		assertThat(message(item)).isEqualTo("엔진오일 교체주기를 5100km 초과했습니다.");
	}

	@Test
	void dateOnlyWarningNeverPrintsNullMileage() {
		var item = item(10000, null, today.minusMonths(1).plusDays(5), 1);
		assertThat(message(item)).contains("교체 예정일은", today.plusDays(5).toString()).doesNotContain("null");
	}

	private String message(MaintenanceItem item) {
		return NotificationMessage.build(item, calculator.calculate(item, today), today);
	}

	private MaintenanceItem item(int mileage, Integer intervalKm, LocalDate replacedOn, Integer intervalMonths) {
		var owner = new AppUser("notification@example.test", "unused", "테스트");
		var vehicle = new Vehicle(owner, "현대", "아반떼", 2023, mileage, 1000);
		return new MaintenanceItem(vehicle, MaintenanceCategory.ENGINE_OIL, null, replacedOn, 0, intervalKm, intervalMonths);
	}
}
