package com.portfolio.drivecare.maintenance;

import com.portfolio.drivecare.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.portfolio.drivecare.maintenance.MaintenanceDtos.CreateMaintenanceRequest;
import com.portfolio.drivecare.maintenance.MaintenanceDtos.MaintenanceResponse;
import com.portfolio.drivecare.maintenance.MaintenanceDtos.PurchaseLinkRequest;
import com.portfolio.drivecare.maintenance.MaintenanceDtos.ReplacementRequest;
import com.portfolio.drivecare.maintenance.MaintenanceDtos.ReplacementResponse;

@RestController
@RequestMapping("/api/maintenance-items")
public class MaintenanceController {
	private final MaintenanceService service;

	public MaintenanceController(MaintenanceService service) {
		this.service = service;
	}

	@GetMapping
	List<MaintenanceResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		return service.list(principal.id());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	MaintenanceResponse create(@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody CreateMaintenanceRequest request) {
		return service.create(principal.id(), request);
	}

	@PatchMapping("/{itemId}/purchase-link")
	MaintenanceResponse updatePurchaseLink(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long itemId, @Valid @RequestBody PurchaseLinkRequest request) {
		return service.updatePurchaseLink(principal.id(), itemId, request);
	}

	@PostMapping("/{itemId}/replacements")
	@ResponseStatus(HttpStatus.CREATED)
	MaintenanceResponse replace(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long itemId, @Valid @RequestBody ReplacementRequest request) {
		return service.replace(principal.id(), itemId, request);
	}

	@GetMapping("/{itemId}/replacements")
	List<ReplacementResponse> history(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long itemId) {
		return service.history(principal.id(), itemId);
	}

	@PostMapping("/{itemId}/repurchase-clicks")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void recordRepurchaseClick(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long itemId) {
		service.recordRepurchaseClick(principal.id(), itemId);
	}

	@DeleteMapping("/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long itemId,
			@RequestParam long expectedRevision) {
		service.delete(principal.id(), itemId, expectedRevision);
	}
}
