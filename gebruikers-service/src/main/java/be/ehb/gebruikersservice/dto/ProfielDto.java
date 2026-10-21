package be.ehb.gebruikersservice.dto;

import be.ehb.gebruikersservice.model.Gebruiker;
import be.ehb.gebruikersservice.model.Rol;

import java.util.List;

public record ProfielDto(
        Long id,
        String gebruikersnaam,
        String email,
        String voornaam,
        String achternaam,
        Rol rol,
        List<GekochtBoekDto> gekochteBoeken
) {

    public static ProfielDto van(Gebruiker gebruiker) {
        return new ProfielDto(
                gebruiker.getId(),
                gebruiker.getGebruikersnaam(),
                gebruiker.getEmail(),
                gebruiker.getVoornaam(),
                gebruiker.getAchternaam(),
                gebruiker.getRol(),
                gebruiker.getGekochteBoeken().stream().map(GekochtBoekDto::van).toList()
        );
    }
}
