package be.ehb.gebruikersservice.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Gebruikersnaam is verplicht")
        String gebruikersnaam,

        @NotBlank(message = "Wachtwoord is verplicht")
        String wachtwoord
) {
}
