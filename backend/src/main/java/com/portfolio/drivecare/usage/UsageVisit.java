package com.portfolio.drivecare.usage;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usage_visits")
public class UsageVisit {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "user_id", nullable = false)
	private Long userId;
	@Column(name = "visited_at", nullable = false, updatable = false)
	private Instant visitedAt;
	protected UsageVisit() { }
	public UsageVisit(Long userId) {
		this.userId = userId;
		this.visitedAt = Instant.now();
	}
}
