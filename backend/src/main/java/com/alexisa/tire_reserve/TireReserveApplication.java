package com.alexisa.tire_reserve;

import com.alexisa.tire_reserve.config.WorkshopsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.function.*;

import java.util.Set;

@SpringBootApplication
@EnableConfigurationProperties(WorkshopsProperties.class)
public class TireReserveApplication {

	public static void main(String[] args) {
		SpringApplication.run(TireReserveApplication.class, args);
	}

	@Bean
	public RouterFunction<ServerResponse> viewRouter() {
		ClassPathResource index = new ClassPathResource("static/index.html");
		Set<String> extensions = Set.of("js", "css", "ico", "png", "ttf", "eot", "woff2", "woff");
		RequestPredicate viewPredicate = RequestPredicates
				.path("/api/**")
				.or(RequestPredicates.pathExtension((ext) -> ext != null && extensions.contains(ext)))
				.negate();

		return RouterFunctions.route().resource(viewPredicate, index).build();
	}

}
