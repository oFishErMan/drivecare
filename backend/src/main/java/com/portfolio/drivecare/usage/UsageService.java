package com.portfolio.drivecare.usage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageService {
	private final UsageVisitRepository visits;
	public UsageService(UsageVisitRepository visits) { this.visits = visits; }
	@Transactional
	public void recordVisit(Long userId) { visits.save(new UsageVisit(userId)); }
}
