package be.ehb.gebruikersservice.client;

import be.ehb.gebruikersservice.exception.CatalogusNietBereikbaarException;
import be.ehb.gebruikersservice.exception.NietGevondenException;
import be.ehb.gebruikersservice.exception.OnvoldoendeStockException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.function.Supplier;

/**
 * Communicatie met de catalogus-service via REST. Fouten van de catalogus worden vertaald
 * naar exceptions van deze service, zodat de rest van de code niets van HTTP hoeft te weten.
 */
@Component
public class CatalogusClient {

    static final String API_KEY_HEADER = "X-API-KEY";

    private final RestClient restClient;

    public CatalogusClient(RestClient.Builder builder,
                           @Value("${app.catalogus.url}") String baseUrl,
                           @Value("${app.catalogus.api-key}") String apiKey) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .build();
    }

    public BoekInfo haalBoek(String isbn) {
        return voerUit(() -> restClient.get()
                .uri("/api/boeken/{isbn}", isbn)
                .retrieve()
                .body(BoekInfo.class));
    }

    /** Vermindert de stock in de catalogus voor alle lijnen samen (alles of niets). */
    public void verminderStock(List<StockLijn> lijnen) {
        voerUit(() -> restClient.post()
                .uri("/api/intern/stock/verminderen")
                .body(new StockVermindering(lijnen))
                .retrieve()
                .toBodilessEntity());
    }

    public void plaatsReview(String isbn, CatalogusReview review) {
        voerUit(() -> restClient.post()
                .uri("/api/intern/boeken/{isbn}/reviews", isbn)
                .body(review)
                .retrieve()
                .toBodilessEntity());
    }

    private static <T> T voerUit(Supplier<T> verzoek) {
        try {
            return verzoek.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NietGevondenException(detail(e));
        } catch (HttpClientErrorException.Conflict e) {
            throw new OnvoldoendeStockException(detail(e));
        } catch (HttpClientErrorException.BadRequest e) {
            throw new IllegalArgumentException(detail(e));
        } catch (RestClientResponseException e) {
            throw new CatalogusNietBereikbaarException(
                    "De catalogus-service gaf een onverwachte fout (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (ResourceAccessException e) {
            throw new CatalogusNietBereikbaarException(e);
        }
    }

    /** Haalt de leesbare foutboodschap uit het ProblemDetail-antwoord van de catalogus. */
    private static String detail(RestClientResponseException e) {
        try {
            Fout fout = e.getResponseBodyAs(Fout.class);
            if (fout != null && fout.detail() != null) {
                return fout.detail();
            }
        } catch (RuntimeException ignored) {
            // geen leesbaar antwoord: val terug op de HTTP-status
        }
        return e.getStatusText();
    }

    record StockVermindering(List<StockLijn> lijnen) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Fout(String title, String detail) {
    }
}
