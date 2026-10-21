package be.ehb.gebruikersservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @Min(value = 1, message = "Score moet tussen 1 en 5 liggen")
        @Max(value = 5, message = "Score moet tussen 1 en 5 liggen")
        int score,

        @Size(max = 2000, message = "Een review mag maximaal 2000 tekens bevatten")
        String tekst
) {
}
