package cl.duoc.api_cloud_native;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class ApiCloudNativeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiCloudNativeApplication.class, args);
	}

	// Agrega este Bean para poder inyectarlo en el controlador
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}