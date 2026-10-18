package com.portfolio.drivecare.vehicle;

import com.portfolio.drivecare.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.portfolio.drivecare.vehicle.VehicleDtos.CreateVehicleRequest;
import com.portfolio.drivecare.vehicle.VehicleDtos.RecordOdometerRequest;
import com.portfolio.drivecare.vehicle.VehicleDtos.VehicleResponse;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
	private final VehicleService service;

	public VehicleController(VehicleService service) {
		this.service = service;
	}

	@GetMapping
	List<VehicleResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		return service.list(principal.id());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	VehicleResponse create(@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody CreateVehicleRequest request) {
		return service.create(principal.id(), request);
	}

	@PostMapping("/{vehicleId}/odometer")
	VehicleResponse recordOdometer(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long vehicleId, @Valid @RequestBody RecordOdometerRequest request) {
		return service.recordOdometer(principal.id(), vehicleId, request);
	}
}
