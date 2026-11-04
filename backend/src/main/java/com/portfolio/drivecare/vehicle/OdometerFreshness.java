package com.portfolio.drivecare.vehicle;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record OdometerFreshness(LocalDate observedOn, Long ageDays, State state) {
	static final int STALE_AFTER_DAYS = 30;

	static OdometerFreshness evaluate(LocalDate observedOn, LocalDate today) {
		if (observedOn == null || observedOn.isAfter(today)) {
			return new OdometerFreshness(null, null, State.UNKNOWN);
		}
		long age = ChronoUnit.DAYS.between(observedOn, today);
		return new OdometerFreshness(observedOn, age,
				age >= STALE_AFTER_DAYS ? State.STALE : State.RECENT);
	}

	public enum State { RECENT, STALE, UNKNOWN }
}
