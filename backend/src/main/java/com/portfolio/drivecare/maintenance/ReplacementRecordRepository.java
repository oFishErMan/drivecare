package com.portfolio.drivecare.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReplacementRecordRepository extends JpaRepository<ReplacementRecord, Long> {
	List<ReplacementRecord> findAllByMaintenanceItemIdOrderByReplacedOnDescIdDesc(Long itemId);

	@Query("""
			select new com.portfolio.drivecare.maintenance.VehicleHistoryEntry(
				record.id, record.replacedOn, record.odometerKm, item.category, record.productName,
				record.intervalKm, record.intervalMonths, item.active)
			from ReplacementRecord record join record.maintenanceItem item join item.vehicle vehicle
			where vehicle.id = :vehicleId and vehicle.owner.id = :ownerId
			order by record.replacedOn desc, record.id desc
			""")
	Slice<VehicleHistoryEntry> findVehicleHistory(@Param("vehicleId") Long vehicleId,
			@Param("ownerId") Long ownerId, Pageable pageable);
}
