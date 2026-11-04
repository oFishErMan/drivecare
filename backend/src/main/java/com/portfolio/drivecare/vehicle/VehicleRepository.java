package com.portfolio.drivecare.vehicle;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
	interface VehicleObservation {
		Vehicle getVehicle();
		LocalDate getObservedOn();
	}

	// One statement keeps the displayed mileage and its observation date on the same snapshot.
	@Query("""
			select vehicle as vehicle,
				(select max(entry.recordedOn) from OdometerEntry entry
				 where entry.vehicle = vehicle and entry.odometerKm = vehicle.currentOdometerKm
				 and entry.recordedOn <= :today) as observedOn
			from Vehicle vehicle where vehicle.owner.id = :ownerId
			order by vehicle.createdAt desc, vehicle.id desc
			""")
	List<VehicleObservation> findOwnedWithObservation(@Param("ownerId") Long ownerId, @Param("today") LocalDate today);
	Optional<Vehicle> findByIdAndOwnerId(Long id, Long ownerId);
	boolean existsByIdAndOwnerId(Long id, Long ownerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select vehicle from Vehicle vehicle where vehicle.id = :id and vehicle.owner.id = :ownerId")
	Optional<Vehicle> findByIdAndOwnerIdForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
