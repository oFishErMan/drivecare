package com.portfolio.drivecare.vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "odometer_entries", uniqueConstraints = @UniqueConstraint(name = "uk_odometer_vehicle_date", columnNames = {"vehicle_id", "recorded_on"}))
public class OdometerEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private Vehicle vehicle;

	@Column(name = "recorded_on", nullable = false)
	private LocalDate recordedOn;

	@Column(name = "odometer_km", nullable = false)
	private int odometerKm;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected OdometerEntry() {
	}

	public OdometerEntry(Vehicle vehicle, LocalDate recordedOn, int odometerKm) {
		this.vehicle = vehicle;
		this.recordedOn = recordedOn;
		this.odometerKm = odometerKm;
	}

	@PrePersist
	void prePersist() { createdAt = Instant.now(); }

	public void updateOdometerKm(int odometerKm) {
		this.odometerKm = odometerKm;
	}
}
