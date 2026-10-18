package com.portfolio.drivecare;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class AuthFlowTests {

	@Autowired
	MockMvc mvc;

	@Test
	void securityErrorsUseTheSameRequestIdInBodyAndHeader() throws Exception {
		String requestId = "12345678-1234-1234-1234-123456789abc";
		mvc.perform(get("/api/vehicles").header("X-Request-ID", requestId))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("X-Request-ID", requestId))
				.andExpect(jsonPath("$.requestId").value(requestId))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
		mvc.perform(post("/api/auth/login").header("X-Request-ID", requestId)
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden())
				.andExpect(header().string("X-Request-ID", requestId))
				.andExpect(jsonPath("$.requestId").value(requestId))
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void registerLoginAndReadSession() throws Exception {
		String registerBody = """
				{"email":"driver@example.com","password":"strong-password","displayName":"운전자"}
				""";
		mvc.perform(post("/api/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(registerBody))
				.andExpect(status().isCreated())
				.andExpect(header().exists("X-Request-ID"));

		String loginBody = """
				{"email":"driver@example.com","password":"strong-password"}
				""";
		var loginResult = mvc.perform(post("/api/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(loginBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("운전자"))
				.andReturn();

		mvc.perform(get("/api/auth/me").session((MockHttpSession) loginResult.getRequest().getSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("driver@example.com"));
	}

	@Test
	void validationErrorsContainFieldNames() throws Exception {
		String invalidBody = """
				{"email":"invalid","password":"short","displayName":""}
				""";

		mvc.perform(post("/api/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(invalidBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("입력값을 확인해주세요."))
				.andExpect(jsonPath("$.fieldErrors.email").exists())
				.andExpect(jsonPath("$.fieldErrors.password").exists())
				.andExpect(jsonPath("$.fieldErrors.displayName").exists());
	}

	@Test
	void duplicateRegistrationReturnsConflictWithoutAuthentication() throws Exception {
		String registerBody = """
				{"email":"duplicate@example.com","password":"strong-password","displayName":"운전자"}
				""";

		mvc.perform(post("/api/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(registerBody))
				.andExpect(status().isCreated());

		mvc.perform(post("/api/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(registerBody))
				.andExpect(status().isConflict());
	}

	@Test
	void odometerCannotMoveBackwards() throws Exception {
		String registerBody = """
				{"email":"odometer@example.com","password":"strong-password","displayName":"운전자"}
				""";
		mvc.perform(post("/api/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(registerBody))
				.andExpect(status().isCreated());

		String loginBody = """
				{"email":"odometer@example.com","password":"strong-password"}
				""";
		var loginResult = mvc.perform(post("/api/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(loginBody))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

		String vehicleBody = """
				{"manufacturer":"현대","model":"아반떼","modelYear":2023,
				 "currentOdometerKm":120000,"monthlyAverageKm":1500}
				""";
		var vehicleResult = mvc.perform(post("/api/vehicles").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(vehicleBody))
				.andExpect(status().isCreated())
				.andReturn();
		Number vehicleId = JsonPath.read(vehicleResult.getResponse().getContentAsString(), "$.id");

		String odometerBody = """
				{"recordedOn":"2026-10-04","odometerKm":119999}
				""";
		mvc.perform(post("/api/vehicles/{vehicleId}/odometer", vehicleId.longValue())
				.session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(odometerBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("현재 주행거리는 이전 기록보다 작을 수 없습니다."));
	}

	@Test
	void servicesKeepOtherUsersVehiclesAndMaintenancePrivate() throws Exception {
		MockHttpSession owner = registerAndLogin("owner@example.com");
		MockHttpSession outsider = registerAndLogin("outsider@example.com");
		var created = mvc.perform(post("/api/vehicles").session(owner).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
				{"manufacturer":"현대","model":"아반떼","currentOdometerKm":120000}
				"""))
				.andExpect(status().isCreated()).andReturn();
		Number vehicleId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		String maintenanceBody = """
				{"vehicleId":%d,"category":"ENGINE_OIL","lastReplacedOn":"2026-10-01",
				 "lastReplacedOdometerKm":110000,"intervalKm":8000}
				""".formatted(vehicleId.longValue());
		mvc.perform(post("/api/maintenance-items").session(owner).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(maintenanceBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status.state").value("OVERDUE"))
				.andExpect(jsonPath("$.status.remainingKm").value(-2000));

		mvc.perform(get("/api/vehicles").session(outsider))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/maintenance-items").session(outsider))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(post("/api/vehicles/{id}/odometer", vehicleId).session(outsider).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
				{"recordedOn":"2026-10-07","odometerKm":130000}
				"""))
				.andExpect(status().isNotFound());
		mvc.perform(post("/api/maintenance-items").session(outsider).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(maintenanceBody))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/vehicles").session(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].currentOdometerKm").value(120000));
	}

	@Test
	void serviceRefactorKeepsMaintenanceValidationAndRejectsUnauthenticatedReads() throws Exception {
		mvc.perform(get("/api/vehicles")).andExpect(status().isUnauthorized());
		MockHttpSession session = registerAndLogin("validation@example.com");
		mvc.perform(post("/api/maintenance-items").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
				{"vehicleId":1,"category":"ENGINE_OIL","lastReplacedOn":"2026-10-01",
				 "lastReplacedOdometerKm":0}
				"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.intervalPresent").exists());
	}

	private MockHttpSession registerAndLogin(String email) throws Exception {
		mvc.perform(post("/api/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
				{"email":"%s","password":"strong-password","displayName":"테스트 운전자"}
				""".formatted(email)))
				.andExpect(status().isCreated());
		var result = mvc.perform(post("/api/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
				{"email":"%s","password":"strong-password"}
				""".formatted(email)))
				.andExpect(status().isOk()).andReturn();
		return (MockHttpSession) result.getRequest().getSession();
	}
}
