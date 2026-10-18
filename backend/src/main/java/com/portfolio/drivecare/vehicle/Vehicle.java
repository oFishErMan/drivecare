package com.portfolio.drivecare.vehicle;

import com.portfolio.drivecare.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "vehicles")
public class Vehicle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private AppUser owner;

	@Column(nullable = false, length = 80)
	private String manufacturer;

	@Column(nullable = false, length = 100)
	private String model;

	@Column(name = "model_year")
	private Integer modelYear;

	@Column(name = "current_odometer_km", nullable = false)
	private int currentOdometerKm;

	@Column(name = "manual_monthly_average_km")
	private Integer manualMonthlyAverageKm;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Vehicle() {
	}

	public Vehicle(AppUser owner, String manufacturer, String model, Integer modelYear,
			int currentOdometerKm, Integer manualMonthlyAverageKm) {
		this.owner = owner;
		this.manufacturer = manufacturer;
		this.model = model;
		this.modelYear = modelYear;
		this.currentOdometerKm = currentOdometerKm;
		this.manualMonthlyAverageKm = manualMonthlyAverageKm;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public void updateOdometer(int odometerKm) {
		if (odometerKm < currentOdometerKm) {
			throw new IllegalArgumentException("현재 주행거리는 이전 기록보다 작을 수 없습니다.");
		}
		currentOdometerKm = odometerKm;
	}

	public Long getId() { return id; }
	public AppUser getOwner() { return owner; }
	public String getManufacturer() { return manufacturer; }
	public String getModel() { return model; }
	public Integer getModelYear() { return modelYear; }
	public int getCurrentOdometerKm() { return currentOdometerKm; }
	public Integer getManualMonthlyAverageKm() { return manualMonthlyAverageKm; }
}
