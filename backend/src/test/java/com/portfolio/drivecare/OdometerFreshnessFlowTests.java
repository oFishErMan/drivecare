package com.portfolio.drivecare;

import com.portfolio.drivecare.security.AuthenticatedUser;
import com.portfolio.drivecare.user.AppUser;
import com.portfolio.drivecare.user.AppUserRepository;
import com.portfolio.drivecare.vehicle.OdometerEntry;
import com.portfolio.drivecare.vehicle.OdometerEntryRepository;
import com.portfolio.drivecare.vehicle.Vehicle;
import com.portfolio.drivecare.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OdometerFreshnessFlowTests {
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);

	@TestConfiguration
	static class FixedClockConfig {
		@Bean @Primary
		Clock odometerTestClock() {
			return Clock.fixed(Instant.parse("2026-11-04T15:30:00Z"), ZoneId.of("Asia/Seoul"));
		}
	}

	@Autowired MockMvc mvc;
	@Autowired AppUserRepository users;
	@Autowired VehicleRepository vehicles;
	@Autowired OdometerEntryRepository entries;
	private AppUser user;
	private RequestPostProcessor session;

	@BeforeEach
	void setUp() {
		user = users.save(new AppUser("odometer-freshness@example.test", "unused", "주행거리 검증"));
		session = authentication(UsernamePasswordAuthenticationToken.authenticated(
				new AuthenticatedUser(user.getId(), user.getEmail(), user.getDisplayName()), null, List.of()));
	}

	@Test
	void registrationRecordsAnObservationOnTheSeoulCalendarDay() throws Exception {
		mvc.perform(post("/api/vehicles").with(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"manufacturer\":\"현대\",\"model\":\"아반떼\",\"currentOdometerKm\":120000}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.odometerFreshness.observedOn").value(TODAY.toString()))
				.andExpect(jsonPath("$.odometerFreshness.ageDays").value(0))
				.andExpect(jsonPath("$.odometerFreshness.state").value("RECENT"));
	}

	@Test
	void backdatedHigherMileageDoesNotInheritANewerDateAndSameValueCanBeReconfirmed() throws Exception {
		var vehicle = createVehicle();
		entries.save(new OdometerEntry(vehicle, TODAY, 120000));
		var staleDate = TODAY.minusDays(30);
		mvc.perform(post(odometerPath(vehicle)).with(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"recordedOn\":\"" + staleDate + "\",\"odometerKm\":120100}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.odometerFreshness.state").value("STALE"))
				.andExpect(jsonPath("$.odometerFreshness.observedOn").value(staleDate.toString()));
		mvc.perform(get("/api/vehicles").with(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].currentOdometerKm").value(120100))
				.andExpect(jsonPath("$[0].odometerFreshness.ageDays").value(30));
		mvc.perform(post(odometerPath(vehicle)).with(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"odometerKm\":120100}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.odometerFreshness.state").value("RECENT"))
				.andExpect(jsonPath("$.odometerFreshness.observedOn").value(TODAY.toString()));
		mvc.perform(get("/api/vehicles").with(session))
				.andExpect(jsonPath("$[0].odometerFreshness.ageDays").value(0));
	}

	@Test
	void legacyFutureOrMismatchedRowsDoNotCountAsAnObservation() throws Exception {
		var vehicle = createVehicle();
		entries.save(new OdometerEntry(vehicle, TODAY.minusDays(1), 110000));
		entries.save(new OdometerEntry(vehicle, TODAY.plusDays(1), 120000));
		mvc.perform(get("/api/vehicles").with(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].odometerFreshness.state").value("UNKNOWN"))
				.andExpect(jsonPath("$[0].odometerFreshness.observedOn").isEmpty());
		entries.save(new OdometerEntry(vehicle, TODAY.minusDays(31), 120000));
		mvc.perform(get("/api/vehicles").with(session))
				.andExpect(jsonPath("$[0].odometerFreshness.ageDays").value(31))
				.andExpect(jsonPath("$[0].odometerFreshness.state").value("STALE"));
	}

	@Test
	void futureAndMissingMileageRequestsAreRejectedWithoutChangingTheStoredValue() throws Exception {
		var vehicle = createVehicle();
		for (String body : List.of("{}", "{\"recordedOn\":\"" + TODAY.plusDays(1) + "\",\"odometerKm\":120100}")) {
			mvc.perform(post(odometerPath(vehicle)).with(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
					.content(body)).andExpect(status().isBadRequest());
		}
		mvc.perform(get("/api/vehicles").with(session))
				.andExpect(jsonPath("$[0].currentOdometerKm").value(120000))
				.andExpect(jsonPath("$[0].odometerFreshness.state").value("UNKNOWN"));
	}

	private Vehicle createVehicle() {
		return vehicles.save(new Vehicle(user, "현대", "아반떼", 2023, 120000, null));
	}

	private static String odometerPath(Vehicle vehicle) {
		return "/api/vehicles/" + vehicle.getId() + "/odometer";
	}
}
