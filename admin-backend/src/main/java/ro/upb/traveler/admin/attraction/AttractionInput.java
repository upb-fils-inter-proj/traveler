package ro.upb.traveler.admin.attraction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Body of POST /admin/attractions and PUT /admin/attractions/{id}. */
public record AttractionInput(
    @NotNull @Valid Location location,
    @NotNull @Positive Integer pointValue,
    @Positive Integer visitMinutes,
    @NotNull @Valid Translations translations) {

  public record Location(
      @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
      @NotNull @DecimalMin("-180") @DecimalMax("180") Double lon) {}

  public record Translations(@NotNull @Valid Text ro, @NotNull @Valid Text en) {}

  public record Text(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 4000) String description,
      @Size(max = 20000) String history,
      @Size(max = 20000) String sources) {}
}
