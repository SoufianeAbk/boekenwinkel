package be.ehb.gebruikersservice.dto;

import be.ehb.gebruikersservice.model.Gebruiker;
import be.ehb.gebruikersservice.model.Rol;

public record GebruikerDto(Long id, String gebruikersnaam, String email, String voornaam, String achternaam, Rol rol) {

    public static GebruikerDto van(Gebruiker gebruiker) {
        return new GebruikerDto(
                gebruiker.getId(),
                gebruiker.getGebruikersnaam(),
                gebruiker.getEmail(),
                gebruiker.getVoornaam(),
                gebruiker.getAchternaam(),
                gebruiker.getRol()
        );
    }
}
