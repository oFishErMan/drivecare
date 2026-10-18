package com.portfolio.drivecare.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RequestIdFilterTests {
	@Test
	void preservesProxyIdAndClearsThreadContextEvenOnFailure() {
		var filter = new RequestIdFilter(1000);
		var request = new MockHttpServletRequest("POST", "/api/vehicles");
		var response = new MockHttpServletResponse();
		String id = UUID.randomUUID().toString();
		request.addHeader("X-Request-ID", id);
		assertThrows(IllegalStateException.class, () -> filter.doFilter(request, response, (req, res) -> {
			assertEquals(id, MDC.get("requestId"));
			throw new IllegalStateException("not-for-logs");
		}));
		assertEquals(id, response.getHeader("X-Request-ID"));
		assertNull(MDC.get("requestId"));
	}

	@Test
	void replacesUntrustedHeaderAndDoesNotLogQueryOrBody() throws Exception {
		var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(RequestIdFilter.class);
		var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
		appender.start(); logger.addAppender(appender);
		try {
			var request = new MockHttpServletRequest("POST", "/api/auth/login");
			request.addHeader("X-Request-ID", "secret-id\r\nforged-log");
			request.setQueryString("password=secret-value");
			request.setContent("password=secret-value".getBytes());
			var response = new MockHttpServletResponse();
			new RequestIdFilter(1000).doFilter(request, response, (req, res) -> response.setStatus(401));
			assertDoesNotThrow(() -> UUID.fromString(response.getHeader("X-Request-ID")));
			String output = appender.list.stream().map(event -> event.getFormattedMessage()).reduce("", String::concat);
			assertTrue(output.contains("status=401"));
			assertFalse(output.contains("secret"));
			assertFalse(output.contains("password"));
		} finally { logger.detachAppender(appender); appender.stop(); }
	}
}
