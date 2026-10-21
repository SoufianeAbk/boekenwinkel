package be.ehb.gebruikersservice.exception;

public class OngeldigeLoginException extends RuntimeException {

    public OngeldigeLoginException() {
        super("Gebruikersnaam of wachtwoord is onjuist");
    }
}
