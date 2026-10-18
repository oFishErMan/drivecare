package com.portfolio.drivecare.auth;

import com.portfolio.drivecare.auth.AuthDtos.LoginRequest;
import com.portfolio.drivecare.auth.AuthDtos.RegisterRequest;
import com.portfolio.drivecare.auth.AuthDtos.UserResponse;
import com.portfolio.drivecare.security.AuthenticatedUser;
import com.portfolio.drivecare.user.AppUser;
import com.portfolio.drivecare.user.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.portfolio.drivecare.observability.OperationLog;
import java.util.Locale;

@Service
public class AuthService {

	private final AppUserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;

	public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (users.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
		}
		AppUser saved = users.save(new AppUser(email, passwordEncoder.encode(request.password()), request.displayName().trim()));
		OperationLog.committed(OperationLog.Action.USER_REGISTERED, saved.getId(), saved.getId());
		return UserResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public Authentication authenticate(LoginRequest request) {
		Authentication result = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim().toLowerCase(Locale.ROOT), request.password()));
		AppUser user = users.findByEmail(result.getName()).orElseThrow();
		AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getDisplayName());
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, result.getAuthorities());
	}
}
