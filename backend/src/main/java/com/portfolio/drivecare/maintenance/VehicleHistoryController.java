package com.portfolio.drivecare.maintenance;

import com.portfolio.drivecare.security.AuthenticatedUser;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/history")
public class VehicleHistoryController {
	private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
	private final VehicleHistoryService service;

	public VehicleHistoryController(VehicleHistoryService service) {
		this.service = service;
	}

	@GetMapping
	ResponseEntity<VehicleHistoryService.HistoryPage> list(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long vehicleId, @RequestParam(defaultValue = "0") int page) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(service.list(principal.id(), vehicleId, page));
	}

	@GetMapping("/export")
	ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long vehicleId) {
		var filename = "drivecare-vehicle-" + vehicleId + "-history.csv";
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.contentType(CSV)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
				.body(service.export(principal.id(), vehicleId));
	}
}
