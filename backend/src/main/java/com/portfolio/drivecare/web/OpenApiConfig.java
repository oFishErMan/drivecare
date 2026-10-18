package com.portfolio.drivecare.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

	@Bean
	OpenAPI driveCareOpenApi() {
		return new OpenAPI().info(new Info()
				.title("DriveCare API")
				.version("v1")
				.description("차량 주행거리와 소모품 교체주기를 관리하는 API"));
	}
}
