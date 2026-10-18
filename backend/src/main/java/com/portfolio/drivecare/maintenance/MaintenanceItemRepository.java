package com.portfolio.drivecare.maintenance;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MaintenanceItemRepository extends JpaRepository<MaintenanceItem, Long> {
	Optional<MaintenanceItem> findByIdAndVehicleOwnerIdAndActiveTrue(Long id, Long ownerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select item from MaintenanceItem item where item.id = :id and item.vehicle.owner.id = :ownerId and item.active = true")
	Optional<MaintenanceItem> findOwnedForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);

	@EntityGraph(attributePaths = {"vehicle", "vehicle.owner"})
	List<MaintenanceItem> findAllByVehicleOwnerIdAndActiveTrueOrderByCreatedAtDesc(Long ownerId);

	@Query("select coalesce(max(item.id), 0) from MaintenanceItem item where item.active = true")
	long findMaxActiveId();

	@Query("select item.id from MaintenanceItem item where item.active = true and item.id > :afterId and item.id <= :upperId order by item.id")
	List<Long> findActiveIds(@Param("afterId") long afterId, @Param("upperId") long upperId, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select item from MaintenanceItem item where item.id = :id and item.active = true")
	Optional<MaintenanceItem> findActiveForUpdate(@Param("id") Long id);
}
