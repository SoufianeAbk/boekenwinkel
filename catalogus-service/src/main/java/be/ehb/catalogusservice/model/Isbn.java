package be.ehb.catalogusservice.model;

import java.util.regex.Pattern;

/**
 * Hulpklasse om ISBN's in een vaste vorm op te slaan: zonder streepjes of spaties.
 */
public final class Isbn {

    private static final Pattern GELDIG = Pattern.compile("\\d{9}[\\dX]|\\d{13}");

    private Isbn() {
    }

    public static String normaliseer(String invoer) {
        String isbn = invoer == null ? "" : invoer.replaceAll("[\\s-]", "").toUpperCase();
        if (!GELDIG.matcher(isbn).matches()) {
            throw new IllegalArgumentException("Ongeldig ISBN: '" + invoer + "' (verwacht 10 of 13 cijfers)");
        }
        return isbn;
    }
}
