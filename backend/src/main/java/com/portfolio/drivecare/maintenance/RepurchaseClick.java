package com.portfolio.drivecare.maintenance;

import com.portfolio.drivecare.user.AppUser;
import jakarta.persistence.*;
import java.net.URI;
import java.time.Instant;

@Entity
@Table(name = "repurchase_clicks")
public class RepurchaseClick {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "maintenance_item_id", nullable = false)
	private MaintenanceItem maintenanceItem;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;
	@Column(name = "destination_host", nullable = false, length = 255)
	private String destinationHost;
	@Column(name = "maintenance_state", nullable = false, length = 30)
	private String maintenanceState;
	@Column(name = "clicked_at", nullable = false, updatable = false)
	private Instant clickedAt;

	protected RepurchaseClick() { }
	public RepurchaseClick(MaintenanceItem item, MaintenanceStatusCalculator.State state) {
		maintenanceItem = item;
		user = item.getVehicle().getOwner();
		destinationHost = URI.create(item.getPurchaseUrl()).getHost();
		maintenanceState = state.name();
		clickedAt = Instant.now();
	}
}
