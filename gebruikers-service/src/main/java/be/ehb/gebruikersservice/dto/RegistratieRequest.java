package be.ehb.gebruikersservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistratieRequest(
        @NotBlank(message = "Gebruikersnaam is verplicht")
        @Size(min = 3, max = 50, message = "Gebruikersnaam moet tussen 3 en 50 tekens lang zijn")
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Gebruikersnaam mag enkel letters, cijfers, punt, - en _ bevatten")
        String gebruikersnaam,

        @NotBlank(message = "E-mailadres is verplicht")
        @Email(message = "E-mailadres is ongeldig")
        String email,

        @NotBlank(message = "Voornaam is verplicht")
        @Size(max = 100)
        String voornaam,

        @NotBlank(message = "Achternaam is verplicht")
        @Size(max = 100)
        String achternaam,

        @NotBlank(message = "Wachtwoord is verplicht")
        @Size(min = 8, max = 100, message = "Wachtwoord moet minstens 8 tekens lang zijn")
        String wachtwoord
) {
}
