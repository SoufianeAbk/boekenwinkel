package be.ehb.gebruikersservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfielWijzigRequest(
        @NotBlank(message = "Voornaam is verplicht")
        @Size(max = 100)
        String voornaam,

        @NotBlank(message = "Achternaam is verplicht")
        @Size(max = 100)
        String achternaam,

        @NotBlank(message = "E-mailadres is verplicht")
        @Email(message = "E-mailadres is ongeldig")
        String email
) {
}
