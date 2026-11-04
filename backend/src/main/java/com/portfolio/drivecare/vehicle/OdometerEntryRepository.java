package com.portfolio.drivecare.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface OdometerEntryRepository extends JpaRepository<OdometerEntry, Long> {
	Optional<OdometerEntry> findByVehicleIdAndRecordedOn(Long vehicleId, LocalDate recordedOn);

	@Query("""
			select max(entry.recordedOn) from OdometerEntry entry
			where entry.vehicle.id = :vehicleId and entry.odometerKm = :odometerKm and entry.recordedOn <= :today
			""")
	LocalDate findLatestMatchingDate(@Param("vehicleId") Long vehicleId,
			@Param("odometerKm") int odometerKm, @Param("today") LocalDate today);
}
