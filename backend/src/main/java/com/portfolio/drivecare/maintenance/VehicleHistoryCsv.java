package com.portfolio.drivecare.maintenance;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

final class VehicleHistoryCsv {
	private static final Pattern FORMULA_START =
			Pattern.compile("^(?:[\\t\\r\\n]|[\\s\\p{Z}\\uFEFF]*[=+@＝＋－＠-])");

	private VehicleHistoryCsv() { }

	static byte[] encode(List<VehicleHistoryEntry> records) {
		var csv = new StringBuilder("\uFEFF교체일,종류 코드,제품명,교체 당시 주행거리(km),거리 주기(km),기간 주기(개월),현재 관리 상태\r\n");
		for (var record : records) {
			csv.append(record.replacedOn()).append(',')
					.append(record.category()).append(',')
					.append(textCell(record.productName())).append(',')
					.append(record.odometerKm()).append(',')
					.append(numberCell(record.intervalKm())).append(',')
					.append(numberCell(record.intervalMonths())).append(',')
					.append(record.active() ? "관리 중" : "관리 종료").append("\r\n");
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static String textCell(String value) {
		if (value == null) return "";
		// A visible text marker survives CSV round trips better than a quote-only escape.
		if (FORMULA_START.matcher(value).find()) value = "[텍스트] " + value;
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}

	private static String numberCell(Integer value) {
		return value == null ? "" : value.toString();
	}
}
