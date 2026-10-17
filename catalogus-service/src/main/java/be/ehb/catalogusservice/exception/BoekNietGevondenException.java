package be.ehb.catalogusservice.exception;

public class BoekNietGevondenException extends RuntimeException {

    public BoekNietGevondenException(String isbn) {
        super("Geen boek gevonden met ISBN " + isbn);
    }
}
