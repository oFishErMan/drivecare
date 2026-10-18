package com.portfolio.drivecare.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 80)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role = UserRole.USER;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AppUser() {
	}

	public AppUser(String email, String passwordHash, String displayName) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public Long getId() { return id; }
	public String getEmail() { return email; }
	public String getPasswordHash() { return passwordHash; }
	public String getDisplayName() { return displayName; }
	public UserRole getRole() { return role; }
}
