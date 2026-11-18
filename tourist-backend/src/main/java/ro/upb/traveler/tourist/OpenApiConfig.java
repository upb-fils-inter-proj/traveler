package ro.upb.traveler.tourist;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

  @Bean
  OpenAPI touristBackendOpenApi(@Value("${app.version}") String version) {
    return new OpenAPI()
        .info(
            new Info()
                .title("Traveler tourist backend")
                .description("API of the Traveler mobile app")
                .version(version));
  }
}
