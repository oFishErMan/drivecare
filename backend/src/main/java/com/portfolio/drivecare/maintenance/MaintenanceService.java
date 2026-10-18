package com.portfolio.drivecare.maintenance;

import com.portfolio.drivecare.maintenance.MaintenanceDtos.CreateMaintenanceRequest;
import com.portfolio.drivecare.maintenance.MaintenanceDtos.MaintenanceResponse;
import com.portfolio.drivecare.maintenance.MaintenanceDtos.PurchaseLinkRequest;
import com.portfolio.drivecare.maintenance.MaintenanceDtos.ReplacementRequest;
import com.portfolio.drivecare.maintenance.MaintenanceDtos.ReplacementResponse;
import com.portfolio.drivecare.notification.Notification;
import com.portfolio.drivecare.notification.NotificationRepository;
import com.portfolio.drivecare.vehicle.Vehicle;
import com.portfolio.drivecare.vehicle.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Clock;
import com.portfolio.drivecare.observability.OperationLog;
import java.util.List;

@Service
public class MaintenanceService {

	private final MaintenanceItemRepository items;
	private final VehicleRepository vehicles;
	private final MaintenanceStatusCalculator calculator;
	private final ReplacementRecordRepository replacements;
	private final RepurchaseClickRepository clicks;
	private final NotificationRepository notifications;
	private final Clock clock;

	public MaintenanceService(MaintenanceItemRepository items, VehicleRepository vehicles,
			MaintenanceStatusCalculator calculator, ReplacementRecordRepository replacements,
			RepurchaseClickRepository clicks, NotificationRepository notifications, Clock clock) {
		this.items = items;
		this.vehicles = vehicles;
		this.calculator = calculator;
		this.replacements = replacements;
		this.clicks = clicks;
		this.notifications = notifications;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<MaintenanceResponse> list(Long userId) {
		LocalDate today = LocalDate.now(clock);
		return items.findAllByVehicleOwnerIdAndActiveTrueOrderByCreatedAtDesc(userId).stream()
				.map(item -> MaintenanceResponse.from(item, calculator.calculate(item, today)))
				.toList();
	}

	@Transactional
	public MaintenanceResponse create(Long userId, CreateMaintenanceRequest request) {
		Vehicle vehicle = vehicles.findByIdAndOwnerId(request.vehicleId(), userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "차량을 찾을 수 없습니다."));
		if (request.lastReplacedOdometerKm() > vehicle.getCurrentOdometerKm()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "교체 당시 주행거리는 현재 주행거리보다 클 수 없습니다.");
		}
		MaintenanceItem item = new MaintenanceItem(vehicle, request.category(), request.productName(),
				request.lastReplacedOn(), request.lastReplacedOdometerKm(), request.intervalKm(), request.intervalMonths());
		item.updatePurchaseUrl(request.purchaseUrl());
		items.save(item);
		replacements.save(new ReplacementRecord(item));
		OperationLog.committed(OperationLog.Action.MAINTENANCE_CREATED, userId, item.getId());
		return MaintenanceResponse.from(item, calculator.calculate(item, LocalDate.now(clock)));
	}

	@Transactional
	public MaintenanceResponse updatePurchaseLink(Long userId, Long itemId, PurchaseLinkRequest request) {
		MaintenanceItem item = ownedForUpdate(userId, itemId, request.expectedRevision());
		item.updatePurchaseUrl(request.purchaseUrl());
		items.flush();
		OperationLog.committed(OperationLog.Action.PURCHASE_LINK_UPDATED, userId, itemId);
		return MaintenanceResponse.from(item, calculator.calculate(item, LocalDate.now(clock)));
	}

	@Transactional
	public MaintenanceResponse replace(Long userId, Long itemId, ReplacementRequest request) {
		MaintenanceItem item = ownedForUpdate(userId, itemId, request.expectedRevision());
		if (request.odometerKm() > item.getVehicle().getCurrentOdometerKm()) {
			throw new IllegalArgumentException("교체 당시 주행거리가 현재 기록보다 큽니다. 차량의 현재 주행거리를 먼저 갱신해 주세요.");
		}
		item.recordReplacement(request.replacedOn(), request.odometerKm());
		replacements.save(new ReplacementRecord(item));
		notifications.findAllByMaintenanceItemIdAndReadAtIsNull(itemId).forEach(Notification::markRead);
		items.flush();
		OperationLog.committed(OperationLog.Action.MAINTENANCE_REPLACED, userId, itemId);
		return MaintenanceResponse.from(item, calculator.calculate(item, LocalDate.now(clock)));
	}

	@Transactional(readOnly = true)
	public List<ReplacementResponse> history(Long userId, Long itemId) {
		owned(userId, itemId);
		return replacements.findAllByMaintenanceItemIdOrderByReplacedOnDescIdDesc(itemId).stream()
				.map(ReplacementResponse::from).toList();
	}

	/** Keep historical records while excluding this item from active tracking. */
	@Transactional
	public void delete(Long userId, Long itemId, long expectedRevision) {
		MaintenanceItem item = ownedForUpdate(userId, itemId, expectedRevision);
		item.deactivate();
		notifications.findAllByMaintenanceItemIdAndReadAtIsNull(itemId).forEach(Notification::markRead);
		OperationLog.committed(OperationLog.Action.MAINTENANCE_DELETED, userId, itemId);
	}

	@Transactional
	public void recordRepurchaseClick(Long userId, Long itemId) {
		MaintenanceItem item = owned(userId, itemId);
		if (item.getPurchaseUrl() == null) throw new IllegalArgumentException("먼저 구매 링크를 저장해 주세요.");
		clicks.save(new RepurchaseClick(item, calculator.calculate(item, LocalDate.now(clock)).state()));
	}

	private MaintenanceItem owned(Long userId, Long itemId) {
		return items.findByIdAndVehicleOwnerIdAndActiveTrue(itemId, userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "소모품을 찾을 수 없습니다."));
	}

	private MaintenanceItem ownedForUpdate(Long userId, Long itemId, long expectedRevision) {
		MaintenanceItem item = items.findOwnedForUpdate(itemId, userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "소모품을 찾을 수 없습니다."));
		if (item.getRevision() != expectedRevision) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 요청으로 기록이 변경되었습니다. 새로고침한 뒤 다시 확인해 주세요.");
		}
		return item;
	}

}
