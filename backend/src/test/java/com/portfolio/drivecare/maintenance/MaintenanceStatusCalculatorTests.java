package com.portfolio.drivecare.maintenance;

import com.portfolio.drivecare.vehicle.Vehicle;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MaintenanceStatusCalculatorTests {

	private final MaintenanceStatusCalculator calculator = new MaintenanceStatusCalculator();
	private final LocalDate today = LocalDate.of(2026, 10, 7);

	@Test
	void mileageThresholdsRemainStable() {
		assertEquals(MaintenanceStatusCalculator.State.GOOD, status(6999, 8000, null, today).state());
		assertEquals(MaintenanceStatusCalculator.State.DUE_SOON, status(7000, 8000, null, today).state());
		assertEquals(MaintenanceStatusCalculator.State.OVERDUE, status(8000, 8000, null, today).state());
		assertEquals(-5100, status(13100, 8000, null, today).remainingKm());
	}

	@Test
	void calendarThresholdsRemainStableWithoutMileageInterval() {
		assertEquals(MaintenanceStatusCalculator.State.GOOD, status(0, null, 1, today).state());
		assertEquals(MaintenanceStatusCalculator.State.DUE_SOON, status(0, null, 1, today.minusDays(1)).state());
		assertEquals(MaintenanceStatusCalculator.State.OVERDUE, status(0, null, 1, today.minusMonths(1)).state());
	}

	@Test
	void whicheverIntervalIsReachedFirstDeterminesOverdue() {
		assertEquals(MaintenanceStatusCalculator.State.OVERDUE, status(100, 8000, 12, today.minusYears(1)).state());
		assertEquals(MaintenanceStatusCalculator.State.OVERDUE, status(8000, 8000, 12, today).state());
	}

	@Test
	void mileageEstimateRequiresPositiveMonthlyAverageAndRemainingDistance() {
		assertEquals(today.plusDays(30), calculator.calculate(item(7000, 8000, null, today, 1000), today).estimatedMileageDate());
		assertNull(calculator.calculate(item(7000, 8000, null, today, 0), today).estimatedMileageDate());
		assertNull(calculator.calculate(item(8000, 8000, null, today, 1000), today).estimatedMileageDate());
	}

	@Test
	void calendarIntervalsClampToTheLastValidDayOfTheTargetMonth() {
		assertEquals(LocalDate.of(2026, 2, 28), status(0, null, 1, LocalDate.of(2026, 1, 31)).nextDate());
		assertEquals(LocalDate.of(2024, 2, 29), status(0, null, 1, LocalDate.of(2024, 1, 31)).nextDate());
		assertEquals(LocalDate.of(2025, 2, 28), status(0, null, 12, LocalDate.of(2024, 2, 29)).nextDate());
		assertEquals(LocalDate.of(2027, 2, 28), status(0, null, 2, LocalDate.of(2026, 12, 31)).nextDate());
	}

	private MaintenanceStatusCalculator.Status status(int odometer, Integer km, Integer months, LocalDate replacedOn) {
		return calculator.calculate(item(odometer, km, months, replacedOn, null), today);
	}

	private MaintenanceItem item(int odometer, Integer km, Integer months, LocalDate replacedOn, Integer monthlyAverage) {
		Vehicle vehicle = new Vehicle(null, "현대", "아반떼", 2023, odometer, monthlyAverage);
		return new MaintenanceItem(vehicle, MaintenanceCategory.ENGINE_OIL, "테스트 오일", replacedOn, 0, km, months);
	}
}
