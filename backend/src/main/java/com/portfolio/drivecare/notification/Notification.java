package com.portfolio.drivecare.notification;

import com.portfolio.drivecare.maintenance.MaintenanceItem;
import com.portfolio.drivecare.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "maintenance_item_id", nullable = false)
	private MaintenanceItem maintenanceItem;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 30)
	private NotificationType notificationType;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(nullable = false, length = 500)
	private String message;

	@Column(name = "dedupe_key", nullable = false, unique = true, length = 180)
	private String dedupeKey;

	@Column(name = "read_at")
	private Instant readAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Notification() {
	}

	public Notification(AppUser user, MaintenanceItem maintenanceItem, NotificationType notificationType,
			String title, String message, String dedupeKey) {
		this.user = user;
		this.maintenanceItem = maintenanceItem;
		this.notificationType = notificationType;
		this.title = title;
		this.message = message;
		this.dedupeKey = dedupeKey;
	}

	@PrePersist
	void prePersist() { createdAt = Instant.now(); }

	public void markRead() { readAt = Instant.now(); }

	public Long getId() { return id; }
	public Long getSourceItemId() { return maintenanceItem.getId(); }
	public NotificationType getNotificationType() { return notificationType; }
	public String getTitle() { return title; }
	public String getMessage() { return message; }
	public Instant getReadAt() { return readAt; }
	public Instant getCreatedAt() { return createdAt; }
}
