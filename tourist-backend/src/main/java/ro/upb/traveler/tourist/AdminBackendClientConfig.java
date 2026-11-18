package ro.upb.traveler.tourist;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ro.upb.traveler.tourist.adminclient.ApiClient;
import ro.upb.traveler.tourist.adminclient.api.InternalApi;

/** Client for the internal API of the admin backend, generated from api/internal-api.yaml. */
@Configuration
class AdminBackendClientConfig {

  @Bean
  InternalApi adminBackendInternalApi(
      RestClient.Builder builder, @Value("${traveler.admin-backend.base-url}") String baseUrl) {
    return new InternalApi(new ApiClient(builder.baseUrl(baseUrl).build()));
  }
}
