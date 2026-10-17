package be.ehb.catalogusservice.model;

import be.ehb.catalogusservice.exception.OnvoldoendeStockException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Boek {

    @Id
    @Column(length = 13)
    private String isbn;

    @Column(nullable = false)
    private String titel;

    @ElementCollection
    @CollectionTable(name = "boek_auteur", joinColumns = @JoinColumn(name = "isbn"))
    @OrderColumn(name = "volgorde")
    @Column(name = "auteur", nullable = false)
    private List<String> auteurs = new ArrayList<>();

    @Column(length = 2000)
    private String omschrijving;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prijs;

    @Column(nullable = false)
    private int stock;

    /**
     * Optimistic locking: wanneer twee bevestigingen tegelijk de stock van hetzelfde boek
     * aanpassen, faalt de tweede in plaats van de eerste stilletjes te overschrijven.
     */
    @Version
    private Long versie;

    public Boek(String isbn, String titel, List<String> auteurs, String omschrijving, BigDecimal prijs, int stock) {
        this.isbn = isbn;
        this.titel = titel;
        this.auteurs = new ArrayList<>(auteurs);
        this.omschrijving = omschrijving;
        this.prijs = prijs;
        this.stock = stock;
    }

    public void verhoogStock(int aantal) {
        if (aantal <= 0) {
            throw new IllegalArgumentException("Het aantal moet groter zijn dan 0");
        }
        stock += aantal;
    }

    public void verminderStock(int aantal) {
        if (aantal <= 0) {
            throw new IllegalArgumentException("Het aantal moet groter zijn dan 0");
        }
        if (aantal > stock) {
            throw new OnvoldoendeStockException(isbn, titel, stock, aantal);
        }
        stock -= aantal;
    }
}
