package com.portfolio.drivecare;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;
import com.portfolio.drivecare.user.AppUser;
import com.portfolio.drivecare.user.AppUserRepository;
import com.portfolio.drivecare.vehicle.Vehicle;
import com.portfolio.drivecare.vehicle.VehicleRepository;
import com.portfolio.drivecare.vehicle.VehicleService;
import com.portfolio.drivecare.vehicle.VehicleDtos.CreateVehicleRequest;
import com.portfolio.drivecare.vehicle.VehicleDtos.RecordOdometerRequest;
import com.portfolio.drivecare.vehicle.OdometerFreshness;
import com.portfolio.drivecare.maintenance.MaintenanceService;
import com.portfolio.drivecare.maintenance.VehicleHistoryService;
import com.portfolio.drivecare.maintenance.MaintenanceCategory;
import com.portfolio.drivecare.maintenance.MaintenanceDtos.CreateMaintenanceRequest;
import com.portfolio.drivecare.maintenance.MaintenanceDtos.ReplacementRequest;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class RealInfrastructureIntegrationTests {

	@Container
	@ServiceConnection
	static final MariaDBContainer mariaDb = new MariaDBContainer(DockerImageName.parse("mariadb:11.8"));

	@Container
	@ServiceConnection(name = "redis")
	static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.2-alpine"))
			.withExposedPorts(6379);

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	StringRedisTemplate redisTemplate;
	@Autowired AppUserRepository users;
	@Autowired VehicleRepository vehicles;
	@Autowired VehicleService vehicleService;
	@Autowired java.time.Clock clock;
	@Autowired MaintenanceService maintenance;
	@Autowired VehicleHistoryService history;
	@Autowired com.portfolio.drivecare.notification.MaintenanceNotificationService notifications;

	@Test
	void simultaneousNotificationJobsCreateOnlyOneNotification() throws Exception {
		var user = users.save(new AppUser("notification-race@example.test", "unused", "알림 동시성 테스트"));
		var vehicle = vehicles.save(new Vehicle(user, "현대", "아반떼", 2023, 120000, 1000));
		LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
		var item = maintenance.create(user.getId(), new CreateMaintenanceRequest(vehicle.getId(),
				MaintenanceCategory.ENGINE_OIL, "오일", today.minusMonths(2), 110000, 8000, 12, null));
		var executor = Executors.newFixedThreadPool(2);
		var start = new CountDownLatch(1);
		java.util.concurrent.Callable<Boolean> action = () -> {
			start.await();
			return notifications.createForItem(item.id(), today);
		};
		try {
			var first = executor.submit(action);
			var second = executor.submit(action);
			start.countDown();
			assertThat(java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder(true, false);
			assertThat(jdbc.queryForObject("select count(*) from notifications where maintenance_item_id = ?",
					Integer.class, item.id())).isEqualTo(1);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void simultaneousReplacementOnlyCreatesOneHistoryRecord() throws Exception {
		AppUser user = users.save(new AppUser("concurrent@example.test", "unused", "동시성 테스트"));
		Vehicle vehicle = vehicles.save(new Vehicle(user, "현대", "아반떼", 2023, 120000, 1000));
		var item = maintenance.create(user.getId(), new CreateMaintenanceRequest(vehicle.getId(),
				MaintenanceCategory.CABIN_FILTER, "필터", LocalDate.now().minusMonths(1), 110000, 8000, 12,
				"https://example.com/filter"));
		var request = new ReplacementRequest(LocalDate.now(), 120000, item.revision());
		var executor = Executors.newFixedThreadPool(2);
		var start = new CountDownLatch(1);
		java.util.concurrent.Callable<Integer> action = () -> {
			start.await();
			try { maintenance.replace(user.getId(), item.id(), request); return 201; }
			catch (ResponseStatusException exception) { return exception.getStatusCode().value(); }
		};
		try {
			var first = executor.submit(action);
			var second = executor.submit(action);
			start.countDown();
			assertThat(java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder(201, 409);
			assertThat(maintenance.history(user.getId(), item.id())).hasSize(2);
			assertThat(maintenance.list(user.getId()).get(0).status().remainingKm()).isEqualTo(8000);
		} finally { executor.shutdownNow(); }
	}

	@Test
	void flywaySchemaAndRedisAreAvailable() {
		Integer migrationCount = jdbc.queryForObject("select count(*) from flyway_schema_history where success = true",
				Integer.class);
		assertThat(migrationCount).isPositive();

		redisTemplate.opsForValue().set("drivecare:integration-test", "ok");
		assertThat(redisTemplate.opsForValue().get("drivecare:integration-test")).isEqualTo("ok");
	}

	@Test
	void archivedReplacementIsProjectedAndExportedOnMariaDb() {
		var user = users.save(new AppUser("history-integration@example.test", "unused", "이력 검증"));
		var vehicle = vehicles.save(new Vehicle(user, "현대", "아반떼", 2023, 120000, null));
		var item = maintenance.create(user.getId(), new CreateMaintenanceRequest(vehicle.getId(),
				MaintenanceCategory.ENGINE_OIL, "엔진오일", LocalDate.of(2025, 1, 1), 110000, 8000, null, null));
		maintenance.delete(user.getId(), item.id(), item.revision());
		assertThat(history.list(user.getId(), vehicle.getId(), 0).entries()).singleElement().satisfies(entry -> {
			assertThat(entry.active()).isFalse();
			assertThat(entry.odometerKm()).isEqualTo(110000);
		});
		assertThat(new String(history.export(user.getId(), vehicle.getId()), java.nio.charset.StandardCharsets.UTF_8))
				.contains("2025-01-01,ENGINE_OIL,\"엔진오일\",110000,8000,,관리 종료");
	}

	@Test
	void observationProjectionMatchesTheCurrentMileageOnMariaDb() {
		var user = users.save(new AppUser("odometer-integration@example.test", "unused", "계기판 검증"));
		var vehicle = vehicleService.create(user.getId(), new CreateVehicleRequest("현대", "아반떼", 2023, 120000, null));
		var oldDay = LocalDate.now(clock).minusDays(31);
		var changed = vehicleService.recordOdometer(user.getId(), vehicle.id(), new RecordOdometerRequest(oldDay, 120100));
		assertThat(changed.odometerFreshness().observedOn()).isEqualTo(oldDay);
		assertThat(vehicleService.list(user.getId())).singleElement().satisfies(row -> {
			assertThat(row.currentOdometerKm()).isEqualTo(120100);
			assertThat(row.odometerFreshness().state()).isEqualTo(OdometerFreshness.State.STALE);
		});
		var confirmed = vehicleService.recordOdometer(user.getId(), vehicle.id(), new RecordOdometerRequest(null, 120100));
		assertThat(confirmed.odometerFreshness().ageDays()).isZero();
		assertThat(vehicleService.list(user.getId()).get(0).odometerFreshness().state())
				.isEqualTo(OdometerFreshness.State.RECENT);
	}
}
