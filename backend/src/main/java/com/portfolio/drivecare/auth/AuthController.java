package com.portfolio.drivecare.auth;

import com.portfolio.drivecare.security.AuthenticatedUser;
import com.portfolio.drivecare.auth.AuthDtos.LoginRequest;
import com.portfolio.drivecare.auth.AuthDtos.RegisterRequest;
import com.portfolio.drivecare.auth.AuthDtos.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService service;
	private final SecurityContextRepository securityContextRepository;

	public AuthController(AuthService service, SecurityContextRepository securityContextRepository) {
		this.service = service;
		this.securityContextRepository = securityContextRepository;
	}

	@GetMapping("/csrf")
	Map<String, String> csrf(CsrfToken token) {
		return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return service.register(request);
	}

	@PostMapping("/login")
	UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		Authentication sessionAuthentication = service.authenticate(request);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(sessionAuthentication);
		SecurityContextHolder.setContext(context);
		servletRequest.getSession(true);
		servletRequest.changeSessionId();
		securityContextRepository.saveContext(context, servletRequest, servletResponse);
		AuthenticatedUser principal = (AuthenticatedUser) sessionAuthentication.getPrincipal();
		return new UserResponse(principal.id(), principal.email(), principal.displayName());
	}

	@GetMapping("/me")
	UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
		return new UserResponse(principal.id(), principal.email(), principal.displayName());
	}

}
