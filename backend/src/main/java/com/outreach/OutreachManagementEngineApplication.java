package com.outreach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OutreachManagementEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(OutreachManagementEngineApplication.class, args);
	}

}
