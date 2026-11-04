package com.portfolio.drivecare.maintenance;

import java.time.LocalDate;

/** Past replacement values; the category is immutable and active is the current item state. */
public record VehicleHistoryEntry(Long id, LocalDate replacedOn, int odometerKm,
		MaintenanceCategory category, String productName, Integer intervalKm,
		Integer intervalMonths, boolean active) { }
