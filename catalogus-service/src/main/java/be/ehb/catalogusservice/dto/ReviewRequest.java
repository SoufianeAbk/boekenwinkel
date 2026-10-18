package be.ehb.catalogusservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Review zoals de gebruikers-service die doorstuurt. Die service heeft al gecontroleerd
 * dat de gebruiker het boek effectief gekocht heeft.
 */
public record ReviewRequest(
        @NotNull(message = "Gebruiker is verplicht")
        Long gebruikerId,

        @NotBlank(message = "Gebruikersnaam is verplicht")
        String gebruikersnaam,

        @Min(value = 1, message = "Score moet tussen 1 en 5 liggen")
        @Max(value = 5, message = "Score moet tussen 1 en 5 liggen")
        int score,

        @Size(max = 2000, message = "Een review mag maximaal 2000 tekens bevatten")
        String tekst
) {
}
