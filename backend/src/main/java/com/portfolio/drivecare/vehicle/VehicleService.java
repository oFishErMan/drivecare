package com.portfolio.drivecare.vehicle;

import com.portfolio.drivecare.user.AppUser;
import com.portfolio.drivecare.user.AppUserRepository;
import com.portfolio.drivecare.vehicle.VehicleDtos.CreateVehicleRequest;
import com.portfolio.drivecare.vehicle.VehicleDtos.RecordOdometerRequest;
import com.portfolio.drivecare.vehicle.VehicleDtos.VehicleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Clock;
import com.portfolio.drivecare.observability.OperationLog;
import java.util.List;

@Service
public class VehicleService {

	private final VehicleRepository vehicles;
	private final OdometerEntryRepository odometerEntries;
	private final AppUserRepository users;
	private final Clock clock;

	public VehicleService(VehicleRepository vehicles, OdometerEntryRepository odometerEntries,
			AppUserRepository users, Clock clock) {
		this.vehicles = vehicles;
		this.odometerEntries = odometerEntries;
		this.users = users;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<VehicleResponse> list(Long userId) {
		LocalDate today = LocalDate.now(clock);
		return vehicles.findOwnedWithObservation(userId, today).stream()
				.map(row -> VehicleResponse.from(row.getVehicle(), row.getObservedOn(), today))
				.toList();
	}

	@Transactional
	public VehicleResponse create(Long userId, CreateVehicleRequest request) {
		LocalDate today = LocalDate.now(clock);
		AppUser owner = users.getReferenceById(userId);
		Vehicle vehicle = vehicles.save(new Vehicle(owner, request.manufacturer().trim(), request.model().trim(),
				request.modelYear(), request.currentOdometerKm(), request.monthlyAverageKm()));
		odometerEntries.save(new OdometerEntry(vehicle, today, request.currentOdometerKm()));
		OperationLog.committed(OperationLog.Action.VEHICLE_CREATED, userId, vehicle.getId());
		return VehicleResponse.from(vehicle, today, today);
	}

	@Transactional
	public VehicleResponse recordOdometer(Long userId, Long vehicleId, RecordOdometerRequest request) {
		Vehicle vehicle = ownedVehicleForUpdate(vehicleId, userId);
		LocalDate today = LocalDate.now(clock);
		LocalDate recordedOn = request.recordedOn() == null ? today : request.recordedOn();
		if (recordedOn.isAfter(today)) {
			throw new IllegalArgumentException("미래 날짜의 주행거리는 기록할 수 없습니다.");
		}
		vehicle.updateOdometer(request.odometerKm());
		OdometerEntry entry = odometerEntries.findByVehicleIdAndRecordedOn(vehicleId, recordedOn)
				.orElseGet(() -> new OdometerEntry(vehicle, recordedOn, request.odometerKm()));
		entry.updateOdometerKm(request.odometerKm());
		odometerEntries.save(entry);
		OperationLog.committed(OperationLog.Action.ODOMETER_UPDATED, userId, vehicleId);
		LocalDate observedOn = odometerEntries.findLatestMatchingDate(vehicleId, request.odometerKm(), today);
		return VehicleResponse.from(vehicle, observedOn, today);
	}

	private Vehicle ownedVehicleForUpdate(Long vehicleId, Long userId) {
		return vehicles.findByIdAndOwnerIdForUpdate(vehicleId, userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "차량을 찾을 수 없습니다."));
	}

}
