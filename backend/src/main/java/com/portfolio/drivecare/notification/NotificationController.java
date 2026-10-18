package com.portfolio.drivecare.notification;

import com.portfolio.drivecare.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.portfolio.drivecare.notification.NotificationDtos.NotificationResponse;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
	private final NotificationService service;

	public NotificationController(NotificationService service) {
		this.service = service;
	}

	@GetMapping
	List<NotificationResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		return service.list(principal.id());
	}

	@PatchMapping("/{notificationId}/read")
	NotificationResponse markRead(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long notificationId) {
		return service.markRead(principal.id(), notificationId);
	}
}
