package com.portfolio.drivecare.vehicle;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
	List<Vehicle> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
	Optional<Vehicle> findByIdAndOwnerId(Long id, Long ownerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select vehicle from Vehicle vehicle where vehicle.id = :id and vehicle.owner.id = :ownerId")
	Optional<Vehicle> findByIdAndOwnerIdForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
