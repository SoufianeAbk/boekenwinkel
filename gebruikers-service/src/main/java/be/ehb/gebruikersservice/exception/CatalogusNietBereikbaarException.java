package be.ehb.gebruikersservice.exception;

public class CatalogusNietBereikbaarException extends RuntimeException {

    public CatalogusNietBereikbaarException(Throwable oorzaak) {
        this("De catalogus-service is momenteel niet bereikbaar. Probeer later opnieuw.", oorzaak);
    }

    public CatalogusNietBereikbaarException(String boodschap, Throwable oorzaak) {
        super(boodschap, oorzaak);
    }
}
