package be.ehb.gebruikersservice.client;

/** Review zoals ze naar de catalogus-service doorgestuurd wordt. */
public record CatalogusReview(Long gebruikerId, String gebruikersnaam, int score, String tekst) {
}
