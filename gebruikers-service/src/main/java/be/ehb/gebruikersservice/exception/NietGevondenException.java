package be.ehb.gebruikersservice.exception;

public class NietGevondenException extends RuntimeException {

    public NietGevondenException(String boodschap) {
        super(boodschap);
    }
}
