package be.ehb.gebruikersservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Reservatie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "gebruiker_id")
    private Gebruiker gebruiker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservatieStatus status = ReservatieStatus.GERESERVEERD;

    @Column(nullable = false)
    private LocalDateTime aangemaaktOp;

    private LocalDateTime bevestigdOp;

    /** Vanaf deze datum mag de klant de boeken afhalen en betalen. */
    private LocalDate afhaalDatum;

    @OneToMany(mappedBy = "reservatie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservatieLijn> lijnen = new ArrayList<>();

    public Reservatie(Gebruiker gebruiker, LocalDateTime aangemaaktOp) {
        this.gebruiker = gebruiker;
        this.aangemaaktOp = aangemaaktOp;
    }

    public void voegLijnToe(String isbn, String titel, BigDecimal prijs, int aantal) {
        lijnen.add(new ReservatieLijn(this, isbn, titel, prijs, aantal));
    }

    public BigDecimal totaal() {
        return lijnen.stream().map(ReservatieLijn::subtotaal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void controleerBevestigbaar() {
        if (status != ReservatieStatus.GERESERVEERD) {
            throw new IllegalStateException("Reservatie #%d kan niet bevestigd worden: status is %s".formatted(id, status));
        }
    }

    public void bevestig(LocalDate afhaalDatum, LocalDateTime tijdstip) {
        controleerBevestigbaar();
        this.status = ReservatieStatus.BEVESTIGD;
        this.afhaalDatum = afhaalDatum;
        this.bevestigdOp = tijdstip;
    }

    public void annuleer() {
        if (status != ReservatieStatus.GERESERVEERD) {
            throw new IllegalStateException("Enkel een openstaande reservatie kan geannuleerd worden");
        }
        this.status = ReservatieStatus.GEANNULEERD;
    }
}
