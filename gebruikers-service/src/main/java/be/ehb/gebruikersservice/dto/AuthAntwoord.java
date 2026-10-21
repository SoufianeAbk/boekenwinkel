package be.ehb.gebruikersservice.dto;

/** Antwoord na registreren of inloggen: de JWT en de gegevens van de gebruiker. */
public record AuthAntwoord(String token, GebruikerDto gebruiker) {
}
