package be.ehb.gebruikersservice.exception;

public class BoekNietGekochtException extends RuntimeException {

    public BoekNietGekochtException(String isbn) {
        super("Je kan enkel een review schrijven voor een boek dat je gekocht hebt (ISBN " + isbn + ")");
    }
}
