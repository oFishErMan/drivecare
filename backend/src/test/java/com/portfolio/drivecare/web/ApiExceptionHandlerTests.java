package com.portfolio.drivecare.web;

import com.portfolio.drivecare.observability.ExceptionDiagnostics;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTests {
	@Test
	void unexpectedErrorsExposeReferenceButNotExceptionDetails() {
		MDC.put("requestId", "0123456789abcdef0123456789abcdef");
		try {
			var exception = new IllegalStateException("password=private", new RuntimeException("secret-sql-value"));
			var response = new ApiExceptionHandler().handleUnexpected(exception);
			assertEquals(500, response.getStatus());
			assertEquals("0123456789abcdef0123456789abcdef", response.getProperties().get("requestId"));
			assertEquals("INTERNAL_ERROR", response.getProperties().get("code"));
			assertFalse(response.toString().contains("private"));
			assertFalse(ExceptionDiagnostics.describe(exception).contains("secret-sql-value"));
			assertFalse(ExceptionDiagnostics.describe(exception).contains("password"));
		} finally { MDC.clear(); }
	}
	@Test
	void loginAndConstraintFailuresKeepTheirExpectedHttpStatus() {
		var handler = new ApiExceptionHandler();
		assertEquals(401, handler.handleAuthentication(new BadCredentialsException("bad")).getStatus());
		var conflict = handler.handleConflict(new DataIntegrityViolationException("email=private@example.test"));
		assertEquals(409, conflict.getStatus());
		assertFalse(conflict.toString().contains("private@example.test"));
	}
}
