package com.portfolio.drivecare.maintenance;

import com.portfolio.drivecare.vehicle.VehicleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VehicleHistoryService {
	private static final int PAGE_SIZE = 20;
	static final int EXPORT_LIMIT = 5_000;
	private final VehicleRepository vehicles;
	private final ReplacementRecordRepository records;

	public VehicleHistoryService(VehicleRepository vehicles, ReplacementRecordRepository records) {
		this.vehicles = vehicles;
		this.records = records;
	}

	@Transactional(readOnly = true)
	public HistoryPage list(Long ownerId, Long vehicleId, int page) {
		if (page < 0 || page > 10_000) throw new IllegalArgumentException("조회할 페이지 번호가 올바르지 않습니다.");
		requireOwnedVehicle(ownerId, vehicleId);
		var rows = records.findVehicleHistory(vehicleId, ownerId, PageRequest.of(page, PAGE_SIZE));
		return new HistoryPage(rows.getContent(), rows.hasNext());
	}

	@Transactional(readOnly = true)
	public byte[] export(Long ownerId, Long vehicleId) {
		requireOwnedVehicle(ownerId, vehicleId);
		var rows = records.findVehicleHistory(vehicleId, ownerId, PageRequest.of(0, EXPORT_LIMIT));
		if (rows.hasNext()) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
					"교체 이력이 5,000건을 초과하여 한 번에 내보낼 수 없습니다.");
		}
		return VehicleHistoryCsv.encode(rows.getContent());
	}

	private void requireOwnedVehicle(Long ownerId, Long vehicleId) {
		if (!vehicles.existsByIdAndOwnerId(vehicleId, ownerId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "차량을 찾을 수 없습니다.");
		}
	}

	public record HistoryPage(List<VehicleHistoryEntry> entries, boolean hasNext) { }
}
