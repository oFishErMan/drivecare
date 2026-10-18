package com.portfolio.drivecare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DriveCareApplication {

	public static void main(String[] args) {
		SpringApplication.run(DriveCareApplication.class, args);
	}
}
