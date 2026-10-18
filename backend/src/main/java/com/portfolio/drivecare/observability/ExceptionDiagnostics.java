package com.portfolio.drivecare.observability;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Exception messages may contain SQL values, URLs or user input. Log types and frames only. */
public final class ExceptionDiagnostics {
	private ExceptionDiagnostics() { }

	public static String describe(Throwable exception) {
		StringBuilder result = new StringBuilder();
		for (int depth = 0; exception != null && depth < 4; depth++, exception = exception.getCause()) {
			if (depth > 0) result.append(" causedBy=");
			result.append(exception.getClass().getName()).append(" frames=")
					.append(Arrays.stream(exception.getStackTrace()).limit(20)
							.map(StackTraceElement::toString).collect(Collectors.joining(" | ")));
		}
		return result.toString();
	}
}
