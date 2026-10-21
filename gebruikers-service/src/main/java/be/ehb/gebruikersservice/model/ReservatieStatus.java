package be.ehb.gebruikersservice.model;

public enum ReservatieStatus {
    /** Door de klant aangevraagd, wacht op bevestiging door de winkel. */
    GERESERVEERD,
    /** Bevestigd door de winkel: de boeken liggen klaar om af te halen en te betalen. */
    BEVESTIGD,
    /** Door de klant geannuleerd voor de bevestiging. */
    GEANNULEERD
}
