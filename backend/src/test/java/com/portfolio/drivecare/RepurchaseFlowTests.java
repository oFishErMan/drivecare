package com.portfolio.drivecare;

import com.jayway.jsonpath.JsonPath;
import com.portfolio.drivecare.security.AuthenticatedUser;
import com.portfolio.drivecare.user.AppUser;
import com.portfolio.drivecare.user.AppUserRepository;
import com.portfolio.drivecare.vehicle.Vehicle;
import com.portfolio.drivecare.vehicle.VehicleRepository;
import com.portfolio.drivecare.maintenance.RepurchaseClickRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RepurchaseFlowTests {
	@Autowired MockMvc mvc;
	@Autowired AppUserRepository users;
	@Autowired VehicleRepository vehicles;
	@Autowired RepurchaseClickRepository clicks;
	@Autowired com.portfolio.drivecare.maintenance.MaintenanceItemRepository items;
	@Autowired com.portfolio.drivecare.maintenance.ReplacementRecordRepository replacements;
	@Autowired com.portfolio.drivecare.notification.NotificationRepository notifications;
	@Autowired com.portfolio.drivecare.notification.MaintenanceNotificationService notificationService;
	private RequestPostProcessor owner;
	private RequestPostProcessor outsider;
	private Long vehicleId;

	@Test
	void deletingAnItemHidesItsNotificationsAndPreservesHistory() throws Exception {
		long id = createItem("https://example.com/filter");
		long keptId = createItem("");
		notificationService.createForItem(id, LocalDate.now());
		notificationService.createForItem(keptId, LocalDate.now());
		long notificationId = notifications.findAllByMaintenanceItemIdAndReadAtIsNull(id).get(0).getId();
		mvc.perform(delete("/api/maintenance-items/{id}", id).param("expectedRevision", "0").with(owner).with(csrf()))
				.andExpect(status().isNoContent());
		items.flush();
		assertEquals(false, items.findById(id).orElseThrow().isActive());
		assertEquals(1, replacements.findAllByMaintenanceItemIdOrderByReplacedOnDescIdDesc(id).size());
		assertEquals(false, notificationService.createForItem(id, LocalDate.now()));
		mvc.perform(get("/api/maintenance-items").with(owner))
				.andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(keptId));
		mvc.perform(get("/api/notifications").with(owner))
				.andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].maintenanceItemId").value(keptId));
		mvc.perform(get("/api/maintenance-items/{id}/replacements", id).with(owner)).andExpect(status().isNotFound());
		mvc.perform(post("/api/maintenance-items/{id}/repurchase-clicks", id).with(owner).with(csrf())).andExpect(status().isNotFound());
		mvc.perform(post("/api/maintenance-items/{id}/replacements", id).with(owner).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(replacement(LocalDate.now(), 120000, 1))).andExpect(status().isNotFound());
		mvc.perform(patch("/api/maintenance-items/{id}/purchase-link", id).with(owner).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"purchaseUrl\":null,\"expectedRevision\":1}")).andExpect(status().isNotFound());
		mvc.perform(delete("/api/maintenance-items/{id}", id).param("expectedRevision", "0").with(owner).with(csrf()))
				.andExpect(status().isNotFound());
		assertEquals(true, notifications.findById(notificationId).orElseThrow().getReadAt() != null);
	}

	@Test
	void deletingRequiresOwnershipLoginCsrfAndCurrentRevision() throws Exception {
		long id = createItem("");
		mvc.perform(delete("/api/maintenance-items/{id}", id).param("expectedRevision", "0").with(csrf()))
				.andExpect(status().isUnauthorized());
		mvc.perform(delete("/api/maintenance-items/{id}", id).param("expectedRevision", "0").with(owner))
				.andExpect(status().isForbidden());
		mvc.perform(delete("/api/maintenance-items/{id}", id).param("expectedRevision", "0").with(outsider).with(csrf()))
				.andExpect(status().isNotFound());
		mvc.perform(delete("/api/maintenance-items/{id}", id).with(owner).with(csrf()))
				.andExpect(status().isBadRequest());
		mvc.perform(patch("/api/maintenance-items/{id}/purchase-link", id).with(owner).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"purchaseUrl\":\"https://example.com/new\",\"expectedRevision\":0}"))
				.andExpect(status().isOk());
		mvc.perform(delete("/api/maintenance-items/{id}", id).param("expectedRevision", "0").with(owner).with(csrf()))
				.andExpect(status().isConflict());
		assertEquals(true, items.findById(id).orElseThrow().isActive());
		mvc.perform(delete("/api/maintenance-items/{id}", id).param("expectedRevision", "1").with(owner).with(csrf()))
				.andExpect(status().isNoContent());
	}

	@BeforeEach
	void setup() {
		AppUser user = users.save(new AppUser("repurchase@example.test", "unused", "재구매 테스트"));
		owner = principal(user);
		outsider = principal(users.save(new AppUser("outsider@example.test", "unused", "다른 사용자")));
		vehicleId = vehicles.save(new Vehicle(user, "현대", "아반떼", 2023, 120000, 1000)).getId();
	}

	@Test
	void replacementPreservesHistoryAndResetsScheduleWithDuplicateProtection() throws Exception {
		long id = createItem("https://example.com/filter");
		String body = replacement(LocalDate.now(), 120000, 0);
		mvc.perform(post("/api/maintenance-items/{id}/replacements", id).with(owner).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status.remainingKm").value(8000))
				.andExpect(jsonPath("$.status.state").value("GOOD"))
				.andExpect(jsonPath("$.revision").value(1))
				.andExpect(jsonPath("$.purchaseUrl").value("https://example.com/filter"));
		mvc.perform(get("/api/maintenance-items/{id}/replacements", id).with(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].odometerKm").value(120000))
				.andExpect(jsonPath("$[1].odometerKm").value(110000));
		mvc.perform(post("/api/maintenance-items/{id}/replacements", id).with(owner).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
	}

	@Test
	void usageVisitsRequireLoginAndCsrf() throws Exception {
		mvc.perform(post("/api/usage/visits").with(csrf())).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/usage/visits").with(owner)).andExpect(status().isForbidden());
		mvc.perform(post("/api/usage/visits").with(owner).with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	void purchaseLinkCanBeEditedRemovedAndClickedButNeverExecutesScripts() throws Exception {
		long id = createItem("");
		mvc.perform(patch("/api/maintenance-items/{id}/purchase-link", id).with(owner).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"purchaseUrl\":\"javascript:alert(1)\",\"expectedRevision\":0}"))
				.andExpect(status().isBadRequest());
		mvc.perform(patch("/api/maintenance-items/{id}/purchase-link", id).with(owner).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"purchaseUrl\":\"https://example.com/filter\",\"expectedRevision\":0}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
		long before = clicks.count();
		mvc.perform(post("/api/maintenance-items/{id}/repurchase-clicks", id).with(owner).with(csrf()))
				.andExpect(status().isNoContent());
		assertEquals(before + 1, clicks.count());
		mvc.perform(patch("/api/maintenance-items/{id}/purchase-link", id).with(owner).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"purchaseUrl\":null,\"expectedRevision\":1}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.purchaseUrl").isEmpty());
		mvc.perform(post("/api/maintenance-items/{id}/repurchase-clicks", id).with(owner).with(csrf()))
				.andExpect(status().isBadRequest());
		assertEquals(before + 1, clicks.count());
	}

	@Test
	void allNewEndpointsEnforceOwnershipAndClickTrackingRequiresCsrf() throws Exception {
		long id = createItem("https://example.com/filter");
		mvc.perform(get("/api/maintenance-items/{id}/replacements", id).with(outsider)).andExpect(status().isNotFound());
		mvc.perform(post("/api/maintenance-items/{id}/repurchase-clicks", id).with(outsider).with(csrf())).andExpect(status().isNotFound());
		mvc.perform(patch("/api/maintenance-items/{id}/purchase-link", id).with(outsider).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"purchaseUrl\":null,\"expectedRevision\":0}")).andExpect(status().isNotFound());
		mvc.perform(post("/api/maintenance-items/{id}/replacements", id).with(outsider).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(replacement(LocalDate.now(), 120000, 0))).andExpect(status().isNotFound());
		mvc.perform(post("/api/maintenance-items/{id}/repurchase-clicks", id).with(owner)).andExpect(status().isForbidden());
	}

	@Test
	void impossibleReplacementDatesAndMileagesAreRejected() throws Exception {
		long id = createItem("");
		for (String body : List.of(replacement(LocalDate.now().plusDays(1), 120000, 0),
				replacement(LocalDate.now(), 120001, 0), replacement(LocalDate.now(), 109999, 0),
				replacement(LocalDate.now().minusMonths(2), 120000, 0))) {
			mvc.perform(post("/api/maintenance-items/{id}/replacements", id).with(owner).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
		}
		mvc.perform(get("/api/maintenance-items/{id}/replacements", id).with(owner))
				.andExpect(jsonPath("$.length()").value(1));
	}

	private long createItem(String url) throws Exception {
		String body = """
				{"vehicleId":%d,"category":"CABIN_FILTER","productName":"테스트 필터",
				 "lastReplacedOn":"%s","lastReplacedOdometerKm":110000,"intervalKm":8000,
				 "intervalMonths":12,"purchaseUrl":"%s"}
				""".formatted(vehicleId, LocalDate.now().minusMonths(1), url);
		var result = mvc.perform(post("/api/maintenance-items").with(owner).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
	}
	private String replacement(LocalDate date, int km, long revision) {
		return "{\"replacedOn\":\"%s\",\"odometerKm\":%d,\"expectedRevision\":%d}".formatted(date, km, revision);
	}
	private RequestPostProcessor principal(AppUser user) {
		return authentication(UsernamePasswordAuthenticationToken.authenticated(
				new AuthenticatedUser(user.getId(), user.getEmail(), user.getDisplayName()), null, List.of()));
	}
}
