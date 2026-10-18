package com.portfolio.drivecare.web;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;

@Component
public class ApiProblems {
	private final ObjectMapper mapper;
	public ApiProblems(ObjectMapper mapper) { this.mapper = mapper; }

	static ProblemDetail create(HttpStatusCode status, String code, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setProperty("code", code);
		problem.setProperty("requestId", requestId());
		return problem;
	}

	public void write(HttpServletResponse response, HttpStatus status, String code, String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType("application/problem+json");
		response.setCharacterEncoding("UTF-8");
		mapper.writeValue(response.getOutputStream(), Map.of("status", status.value(), "title", status.getReasonPhrase(),
				"detail", detail, "code", code, "requestId", requestId()));
	}

	private static String requestId() {
		String value = MDC.get("requestId");
		return value == null ? "unavailable" : value;
	}
}
