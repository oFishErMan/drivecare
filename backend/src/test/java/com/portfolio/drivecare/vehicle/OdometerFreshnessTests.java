package com.portfolio.drivecare.vehicle;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class OdometerFreshnessTests {
	@Test
	void freshnessBoundaryUsesCalendarDaysAndTreatsDayThirtyAsStale() {
		var today = LocalDate.of(2026, 11, 5);
		assertEquals(OdometerFreshness.State.RECENT, OdometerFreshness.evaluate(today.minusDays(29), today).state());
		assertEquals(new OdometerFreshness(today.minusDays(30), 30L, OdometerFreshness.State.STALE),
				OdometerFreshness.evaluate(today.minusDays(30), today));
		assertEquals(0L, OdometerFreshness.evaluate(today, today).ageDays());
	}

	@Test
	void absentAndFutureObservationDatesAreUnknown() {
		var today = LocalDate.of(2026, 11, 5);
		var unknown = new OdometerFreshness(null, null, OdometerFreshness.State.UNKNOWN);
		assertEquals(unknown, OdometerFreshness.evaluate(null, today));
		assertEquals(unknown, OdometerFreshness.evaluate(today.plusDays(1), today));
	}
}
