package com.portfolio.drivecare.maintenance;

import com.portfolio.drivecare.vehicle.Vehicle;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_items")
public class MaintenanceItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private Vehicle vehicle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private MaintenanceCategory category;

	@Column(name = "product_name", length = 160)
	private String productName;

	@Column(name = "purchase_url", length = 2048)
	private String purchaseUrl;

	@Version
	@Column(nullable = false)
	private long revision;

	@Column(name = "last_replaced_on", nullable = false)
	private LocalDate lastReplacedOn;

	@Column(name = "last_replaced_odometer_km", nullable = false)
	private int lastReplacedOdometerKm;

	@Column(name = "interval_km")
	private Integer intervalKm;

	@Column(name = "interval_months")
	private Integer intervalMonths;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MaintenanceItem() {
	}

	public MaintenanceItem(Vehicle vehicle, MaintenanceCategory category, String productName,
			LocalDate lastReplacedOn, int lastReplacedOdometerKm, Integer intervalKm, Integer intervalMonths) {
		this.vehicle = vehicle;
		this.category = category;
		this.productName = productName;
		this.lastReplacedOn = lastReplacedOn;
		this.lastReplacedOdometerKm = lastReplacedOdometerKm;
		this.intervalKm = intervalKm;
		this.intervalMonths = intervalMonths;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void preUpdate() { updatedAt = Instant.now(); }

	public Long getId() { return id; }
	public String getPurchaseUrl() { return purchaseUrl; }
	public long getRevision() { return revision; }
	public void updatePurchaseUrl(String url) { purchaseUrl = PurchaseLink.normalize(url); }

	public void recordReplacement(LocalDate replacedOn, int odometerKm) {
		if (replacedOn.isBefore(lastReplacedOn) || odometerKm < lastReplacedOdometerKm) {
			throw new IllegalArgumentException("최근 교체 기록보다 이전 날짜나 작은 주행거리는 입력할 수 없습니다.");
		}
		if (replacedOn.equals(lastReplacedOn) && odometerKm == lastReplacedOdometerKm) {
			throw new IllegalArgumentException("이미 같은 날짜와 주행거리의 교체 기록이 있습니다.");
		}
		lastReplacedOn = replacedOn;
		lastReplacedOdometerKm = odometerKm;
	}
	public Vehicle getVehicle() { return vehicle; }
	public MaintenanceCategory getCategory() { return category; }
	public String getProductName() { return productName; }
	public LocalDate getLastReplacedOn() { return lastReplacedOn; }
	public int getLastReplacedOdometerKm() { return lastReplacedOdometerKm; }
	public Integer getIntervalKm() { return intervalKm; }
	public Integer getIntervalMonths() { return intervalMonths; }
	public boolean isActive() { return active; }
	public void deactivate() { active = false; }
}
