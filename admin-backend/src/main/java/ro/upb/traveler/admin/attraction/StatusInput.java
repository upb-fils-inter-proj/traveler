package ro.upb.traveler.admin.attraction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Body of PUT /admin/attractions/{id}/status. */
public record StatusInput(
    @NotNull @Pattern(regexp = "open|closed|removed", message = "must be open, closed or removed")
        String status) {}
