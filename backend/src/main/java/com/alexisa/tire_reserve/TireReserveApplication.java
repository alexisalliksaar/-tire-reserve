package com.alexisa.tire_reserve;

import com.alexisa.tire_reserve.config.WorkshopsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WorkshopsProperties.class)
public class TireReserveApplication {

	public static void main(String[] args) {
		SpringApplication.run(TireReserveApplication.class, args);
	}

}
