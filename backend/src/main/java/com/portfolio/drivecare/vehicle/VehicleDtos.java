package com.portfolio.drivecare.vehicle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class VehicleDtos {
	private VehicleDtos() { }

	public record CreateVehicleRequest(
			@NotBlank @Size(max = 80) String manufacturer,
			@NotBlank @Size(max = 100) String model,
			@Min(1950) @Max(2100) Integer modelYear,
			@Min(0) int currentOdometerKm,
			@Min(0) Integer monthlyAverageKm) {
	}

	public record RecordOdometerRequest(
			@NotNull LocalDate recordedOn,
			@Min(0) int odometerKm) {
	}

	public record VehicleResponse(Long id, String manufacturer, String model, Integer modelYear,
			int currentOdometerKm, Integer monthlyAverageKm) {
		static VehicleResponse from(Vehicle vehicle) {
			return new VehicleResponse(vehicle.getId(), vehicle.getManufacturer(), vehicle.getModel(),
					vehicle.getModelYear(), vehicle.getCurrentOdometerKm(), vehicle.getManualMonthlyAverageKm());
		}
	}
}
