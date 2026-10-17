package be.ehb.catalogusservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Een review van een gebruiker op een boek. Per gebruiker is er maximaal één review per boek;
 * een nieuwe review van dezelfde gebruiker overschrijft de vorige.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"boek_isbn", "gebruiker_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "boek_isbn")
    private Boek boek;

    @Column(name = "gebruiker_id", nullable = false)
    private Long gebruikerId;

    @Column(nullable = false)
    private String gebruikersnaam;

    @Column(nullable = false)
    private int score;

    @Column(length = 2000)
    private String tekst;

    @Column(nullable = false)
    private LocalDateTime aangemaaktOp;

    public Review(Boek boek, Long gebruikerId) {
        this.boek = boek;
        this.gebruikerId = gebruikerId;
    }
}
