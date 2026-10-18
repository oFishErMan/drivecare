package com.portfolio.drivecare.maintenance;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_replacements", uniqueConstraints = @UniqueConstraint(
		name = "uk_replacement_item_date_km", columnNames = {"maintenance_item_id", "replaced_on", "odometer_km"}))
public class ReplacementRecord {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "maintenance_item_id", nullable = false)
	private MaintenanceItem maintenanceItem;
	@Column(name = "product_name", length = 160)
	private String productName;
	@Column(name = "purchase_url", length = 2048)
	private String purchaseUrl;
	@Column(name = "replaced_on", nullable = false)
	private LocalDate replacedOn;
	@Column(name = "odometer_km", nullable = false)
	private int odometerKm;
	@Column(name = "interval_km")
	private Integer intervalKm;
	@Column(name = "interval_months")
	private Integer intervalMonths;
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ReplacementRecord() { }
	public ReplacementRecord(MaintenanceItem item) {
		maintenanceItem = item;
		productName = item.getProductName();
		purchaseUrl = item.getPurchaseUrl();
		replacedOn = item.getLastReplacedOn();
		odometerKm = item.getLastReplacedOdometerKm();
		intervalKm = item.getIntervalKm();
		intervalMonths = item.getIntervalMonths();
		createdAt = Instant.now();
	}
	public Long getId() { return id; }
	public String getProductName() { return productName; }
	public LocalDate getReplacedOn() { return replacedOn; }
	public int getOdometerKm() { return odometerKm; }
	public Integer getIntervalKm() { return intervalKm; }
	public Integer getIntervalMonths() { return intervalMonths; }
}
