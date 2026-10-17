package be.ehb.catalogusservice.exception;

import lombok.Getter;

@Getter
public class OnvoldoendeStockException extends RuntimeException {

    private final String isbn;
    private final int beschikbaar;
    private final int gevraagd;

    public OnvoldoendeStockException(String isbn, String titel, int beschikbaar, int gevraagd) {
        super("Onvoldoende stock voor '%s': %d gevraagd, nog %d beschikbaar".formatted(titel, gevraagd, beschikbaar));
        this.isbn = isbn;
        this.beschikbaar = beschikbaar;
        this.gevraagd = gevraagd;
    }
}
