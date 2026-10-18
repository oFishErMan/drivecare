package com.portfolio.drivecare.auth;

import com.portfolio.drivecare.user.AppUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

	private AuthDtos() { }

	public record RegisterRequest(
			@NotBlank @Email String email,
			@NotBlank @Size(min = 8, max = 72) String password,
			@NotBlank @Size(max = 80) String displayName) {
	}

	public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
	}

	public record UserResponse(Long id, String email, String displayName) {
		static UserResponse from(AppUser user) {
			return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
		}
	}
}
