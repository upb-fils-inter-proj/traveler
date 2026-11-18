package ro.upb.traveler.admin;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

  @Bean
  OpenAPI adminBackendOpenApi(@Value("${app.version}") String version) {
    return new OpenAPI()
        .info(
            new Info()
                .title("Traveler admin backend")
                .description("API of the Traveler admin web app")
                .version(version));
  }
}
