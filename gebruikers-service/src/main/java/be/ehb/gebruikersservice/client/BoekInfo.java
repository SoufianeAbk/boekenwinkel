package be.ehb.gebruikersservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/** De gegevens van een boek die de gebruikers-service uit de catalogus nodig heeft. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BoekInfo(String isbn, String titel, List<String> auteurs, BigDecimal prijs, int stock) {
}
