package be.ehb.gebruikersservice.exception;

public class OnvoldoendeStockException extends RuntimeException {

    public OnvoldoendeStockException(String boodschap) {
        super(boodschap);
    }
}
