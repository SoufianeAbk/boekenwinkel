package be.ehb.catalogusservice.exception;

public class BoekBestaatAlException extends RuntimeException {

    public BoekBestaatAlException(String isbn) {
        super("Er bestaat al een boek met ISBN " + isbn);
    }
}
