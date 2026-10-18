package com.portfolio.drivecare.usage;

import com.portfolio.drivecare.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usage")
public class UsageController {
	private final UsageService service;
	public UsageController(UsageService service) { this.service = service; }
	@PostMapping("/visits")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void recordVisit(@AuthenticationPrincipal AuthenticatedUser user) { service.recordVisit(user.id()); }
}
