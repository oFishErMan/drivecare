package com.portfolio.drivecare.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface OdometerEntryRepository extends JpaRepository<OdometerEntry, Long> {
	Optional<OdometerEntry> findByVehicleIdAndRecordedOn(Long vehicleId, LocalDate recordedOn);
}
