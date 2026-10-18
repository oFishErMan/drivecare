package com.portfolio.drivecare.maintenance;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MaintenanceStatusCalculator {
	private static final int DUE_SOON_KM = 1_000;
	public static final int DUE_SOON_DAYS = 30;
	private static final double AVERAGE_DAYS_PER_MONTH = 30.44;

	public Status calculate(MaintenanceItem item, LocalDate today) {
		Integer nextOdometerKm = item.getIntervalKm() == null ? null
				: item.getLastReplacedOdometerKm() + item.getIntervalKm();
		Integer remainingKm = nextOdometerKm == null ? null
				: nextOdometerKm - item.getVehicle().getCurrentOdometerKm();
		LocalDate nextDate = item.getIntervalMonths() == null ? null
				: item.getLastReplacedOn().plusMonths(item.getIntervalMonths());

		boolean overdue = (remainingKm != null && remainingKm <= 0)
				|| (nextDate != null && !nextDate.isAfter(today));
		boolean dueSoon = !overdue && ((remainingKm != null && remainingKm <= DUE_SOON_KM)
				|| (nextDate != null && !nextDate.isAfter(today.plusDays(DUE_SOON_DAYS))));

		LocalDate estimatedMileageDate = null;
		Integer monthlyAverage = item.getVehicle().getManualMonthlyAverageKm();
		if (remainingKm != null && remainingKm > 0 && monthlyAverage != null && monthlyAverage > 0) {
			long days = Math.max(1, Math.round((remainingKm.doubleValue() / monthlyAverage) * AVERAGE_DAYS_PER_MONTH));
			estimatedMileageDate = today.plusDays(days);
		}

		return new Status(nextOdometerKm, remainingKm, nextDate, estimatedMileageDate,
				overdue ? State.OVERDUE : dueSoon ? State.DUE_SOON : State.GOOD);
	}

	public enum State { GOOD, DUE_SOON, OVERDUE }

	public record Status(Integer nextOdometerKm, Integer remainingKm, LocalDate nextDate,
			LocalDate estimatedMileageDate, State state) {
	}
}
