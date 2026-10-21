package be.ehb.gebruikersservice.exception;

public class GebruikerBestaatAlException extends RuntimeException {

    public GebruikerBestaatAlException(String boodschap) {
        super(boodschap);
    }
}
