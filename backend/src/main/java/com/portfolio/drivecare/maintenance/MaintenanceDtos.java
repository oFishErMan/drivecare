package com.portfolio.drivecare.maintenance;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public final class MaintenanceDtos {
	private MaintenanceDtos() { }

	public record CreateMaintenanceRequest(
			@NotNull Long vehicleId,
			@NotNull MaintenanceCategory category,
			@Size(max = 160) String productName,
			@NotNull @PastOrPresent LocalDate lastReplacedOn,
			@Min(0) int lastReplacedOdometerKm,
			@Min(1) Integer intervalKm,
			@Min(1) Integer intervalMonths,
			@Size(max = 2048) String purchaseUrl) {

		@AssertTrue(message = "주행거리 또는 기간 교체주기 중 하나는 필요합니다.")
		public boolean isIntervalPresent() {
			return intervalKm != null || intervalMonths != null;
		}
	}

	public record MaintenanceResponse(Long id, Long vehicleId, MaintenanceCategory category,
			String productName, LocalDate lastReplacedOn, int lastReplacedOdometerKm,
			Integer intervalKm, Integer intervalMonths, String purchaseUrl, long revision,
			MaintenanceStatusCalculator.Status status) {
		static MaintenanceResponse from(MaintenanceItem item, MaintenanceStatusCalculator.Status status) {
			return new MaintenanceResponse(item.getId(), item.getVehicle().getId(), item.getCategory(),
					item.getProductName(), item.getLastReplacedOn(), item.getLastReplacedOdometerKm(),
					item.getIntervalKm(), item.getIntervalMonths(), item.getPurchaseUrl(), item.getRevision(), status);
		}
	}

	public record PurchaseLinkRequest(@Size(max = 2048) String purchaseUrl, @NotNull @Min(0) Long expectedRevision) { }
	public record ReplacementRequest(@NotNull @PastOrPresent LocalDate replacedOn,
			@NotNull @Min(0) Integer odometerKm, @NotNull @Min(0) Long expectedRevision) { }
	public record ReplacementResponse(Long id, String productName, LocalDate replacedOn,
			int odometerKm, Integer intervalKm, Integer intervalMonths) {
		static ReplacementResponse from(ReplacementRecord record) {
			return new ReplacementResponse(record.getId(), record.getProductName(), record.getReplacedOn(),
					record.getOdometerKm(), record.getIntervalKm(), record.getIntervalMonths());
		}
	}
}
