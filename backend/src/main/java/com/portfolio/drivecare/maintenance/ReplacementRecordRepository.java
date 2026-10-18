package com.portfolio.drivecare.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReplacementRecordRepository extends JpaRepository<ReplacementRecord, Long> {
	List<ReplacementRecord> findAllByMaintenanceItemIdOrderByReplacedOnDescIdDesc(Long itemId);
}
