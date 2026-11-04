package com.portfolio.drivecare.maintenance;

import com.portfolio.drivecare.vehicle.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleHistoryExportTests {
	@Test
	void spreadsheetFormulaPrefixesAreMarkedAsTextWithoutChangingStoredRecords() {
		for (String input : List.of("=1+1", " +1", "-1", "@SUM(1)", "　＝1", "\tname", "\nname", "\uFEFF＋1")) {
			var entry = new VehicleHistoryEntry(1L, LocalDate.of(2025, 1, 1), 100,
					MaintenanceCategory.OTHER, input, null, 1, true);
			String csv = new String(VehicleHistoryCsv.encode(List.of(entry)), StandardCharsets.UTF_8);
			assertTrue(csv.contains(",\"[텍스트] " + input + "\",100,,1,관리 중"));
			assertEquals(input, entry.productName());
		}
	}

	@Test
	void exportDoesNotSilentlyTruncateRecordsAboveTheLimit() {
		var vehicles = mock(VehicleRepository.class);
		var records = mock(ReplacementRecordRepository.class);
		var request = PageRequest.of(0, VehicleHistoryService.EXPORT_LIMIT);
		when(vehicles.existsByIdAndOwnerId(2L, 1L)).thenReturn(true);
		when(records.findVehicleHistory(2L, 1L, request)).thenReturn(new SliceImpl<>(List.of(), request, true));
		var error = assertThrows(ResponseStatusException.class,
				() -> new VehicleHistoryService(vehicles, records).export(1L, 2L));
		assertEquals(422, error.getStatusCode().value());
	}
}
