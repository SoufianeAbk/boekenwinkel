package be.ehb.gebruikersservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Gebruiker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String gebruikersnaam;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String voornaam;

    @Column(nullable = false, length = 100)
    private String achternaam;

    @Column(nullable = false)
    private String wachtwoordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.KLANT;

    /** De boeken die deze gebruiker gekocht heeft: zichtbaar in het profiel. */
    @OneToMany(mappedBy = "gebruiker", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("laatstGekochtOp DESC")
    private List<GekochtBoek> gekochteBoeken = new ArrayList<>();

    public Gebruiker(String gebruikersnaam, String email, String voornaam, String achternaam,
                     String wachtwoordHash, Rol rol) {
        this.gebruikersnaam = gebruikersnaam;
        this.email = email;
        this.voornaam = voornaam;
        this.achternaam = achternaam;
        this.wachtwoordHash = wachtwoordHash;
        this.rol = rol;
    }

    public Optional<GekochtBoek> gekochtBoek(String isbn) {
        return gekochteBoeken.stream().filter(boek -> boek.getIsbn().equals(isbn)).findFirst();
    }

    /**
     * Voegt een aankoop toe aan het profiel. Koopt de gebruiker een boek dat hij al eerder kocht,
     * dan wordt het aantal verhoogd in plaats van een tweede lijn te maken.
     */
    public GekochtBoek registreerAankoop(String isbn, String titel, int aantal, LocalDateTime tijdstip) {
        GekochtBoek gekocht = gekochtBoek(isbn).orElseGet(() -> {
            GekochtBoek nieuw = new GekochtBoek(this, isbn, titel);
            gekochteBoeken.add(nieuw);
            return nieuw;
        });
        gekocht.voegAankoopToe(aantal, tijdstip);
        return gekocht;
    }
}
