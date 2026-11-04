package com.portfolio.drivecare;

import com.portfolio.drivecare.maintenance.MaintenanceCategory;
import com.portfolio.drivecare.maintenance.MaintenanceItem;
import com.portfolio.drivecare.maintenance.MaintenanceItemRepository;
import com.portfolio.drivecare.maintenance.ReplacementRecord;
import com.portfolio.drivecare.maintenance.ReplacementRecordRepository;
import com.portfolio.drivecare.security.AuthenticatedUser;
import com.portfolio.drivecare.user.AppUser;
import com.portfolio.drivecare.user.AppUserRepository;
import com.portfolio.drivecare.vehicle.Vehicle;
import com.portfolio.drivecare.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VehicleHistoryFlowTests {
	@Autowired MockMvc mvc;
	@Autowired AppUserRepository users;
	@Autowired VehicleRepository vehicles;
	@Autowired MaintenanceItemRepository items;
	@Autowired ReplacementRecordRepository records;

	private RequestPostProcessor owner;
	private RequestPostProcessor outsider;
	private Vehicle vehicle;
	private Vehicle secondVehicle;
	private static final LocalDate BASE_DATE = LocalDate.of(2025, 1, 1);

	@BeforeEach
	void setUp() {
		var user = users.save(new AppUser("history-owner@example.test", "unused", "운전자"));
		var other = users.save(new AppUser("history-other@example.test", "unused", "다른 운전자"));
		owner = session(user);
		outsider = session(other);
		vehicle = vehicles.save(new Vehicle(user, "현대", "아반떼", 2023, 120000, null));
		secondVehicle = vehicles.save(new Vehicle(user, "기아", "셀토스", 2024, 90000, null));
	}

	@Test
	void historyIsPaginatedAndStableForRecordsOnTheSameDay() throws Exception {
		for (int i = 0; i < 21; i++) addRecord(vehicle, "제품 " + i, BASE_DATE, false);
		addRecord(secondVehicle, "다른 차량", BASE_DATE.plusDays(1), false);
		mvc.perform(get(history()).with(owner))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.entries.length()").value(20))
				.andExpect(jsonPath("$.entries[0].productName").value("제품 20"))
				.andExpect(jsonPath("$.entries[19].productName").value("제품 1"))
				.andExpect(jsonPath("$.hasNext").value(true));
		mvc.perform(get(history()).param("page", "1").with(owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entries.length()").value(1))
				.andExpect(jsonPath("$.entries[0].productName").value("제품 0"))
				.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	void archivedHistoryIsVisibleAndExportExcludesPrivateFieldsAndOtherVehicles() throws Exception {
		addRecord(vehicle, "필터, \"고급\"", BASE_DATE, true);
		addRecord(secondVehicle, "다른 차량", BASE_DATE, false);
		mvc.perform(get(history()).with(owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entries[0].active").value(false))
				.andExpect(jsonPath("$.entries[0].purchaseUrl").doesNotExist());
		var response = mvc.perform(get(history() + "/export").with(owner))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/csv;charset=UTF-8"))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().string("Content-Disposition",
						"attachment; filename=\"drivecare-vehicle-" + vehicle.getId() + "-history.csv\""))
				.andReturn().getResponse();
		var csv = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
		assertTrue(csv.startsWith("\uFEFF교체일"));
		assertTrue(csv.contains("\"필터, \"\"고급\"\"\",110000,8000,,관리 종료"));
		assertFalse(csv.contains("다른 차량"));
		assertFalse(csv.contains("example.test"));
	}

	@Test
	void historyAndExportRequireAuthenticationAndOwnership() throws Exception {
		for (String path : List.of(history(), history() + "/export")) {
			mvc.perform(get(path)).andExpect(status().isUnauthorized());
			mvc.perform(get(path).with(outsider)).andExpect(status().isNotFound());
		}
	}

	@Test
	void emptyVehicleHasNoEntriesAndNegativePageIsRejected() throws Exception {
		mvc.perform(get(history()).with(owner)).andExpect(status().isOk())
				.andExpect(jsonPath("$.entries").isEmpty())
				.andExpect(jsonPath("$.hasNext").value(false));
		mvc.perform(get(history()).param("page", "-1").with(owner)).andExpect(status().isBadRequest());
	}

	private void addRecord(Vehicle target, String name, LocalDate date, boolean archived) {
		var item = new MaintenanceItem(target, MaintenanceCategory.CABIN_FILTER, name, date, 110000, 8000, null);
		item.updatePurchaseUrl("https://seller.example.test/item");
		if (archived) item.deactivate();
		items.save(item);
		records.save(new ReplacementRecord(item));
	}

	private String history() {
		return "/api/vehicles/" + vehicle.getId() + "/history";
	}

	private static RequestPostProcessor session(AppUser user) {
		return authentication(UsernamePasswordAuthenticationToken.authenticated(
				new AuthenticatedUser(user.getId(), user.getEmail(), user.getDisplayName()), null, List.of()));
	}
}
