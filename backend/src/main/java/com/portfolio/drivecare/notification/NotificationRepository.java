package com.portfolio.drivecare.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	List<Notification> findAllByMaintenanceItemIdAndReadAtIsNull(Long itemId);
	boolean existsByDedupeKey(String dedupeKey);
	List<Notification> findAllByUserIdAndMaintenanceItemActiveTrueOrderByCreatedAtDesc(Long userId);
	Optional<Notification> findByIdAndUserIdAndMaintenanceItemActiveTrue(Long id, Long userId);
}
