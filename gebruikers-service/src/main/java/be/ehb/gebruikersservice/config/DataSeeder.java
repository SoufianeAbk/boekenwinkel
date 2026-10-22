package be.ehb.gebruikersservice.config;

import be.ehb.gebruikersservice.model.Gebruiker;
import be.ehb.gebruikersservice.model.Rol;
import be.ehb.gebruikersservice.repository.GebruikerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Maakt bij de eerste start een beheerder en een demo-klant aan.
 * De wachtwoorden staan in application.properties (enkel bedoeld voor lokale ontwikkeling).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final GebruikerRepository gebruikerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-wachtwoord}")
    private String adminWachtwoord;

    @Value("${app.seed.klant-wachtwoord}")
    private String klantWachtwoord;

    @Override
    public void run(String... args) {
        maakAanIndienNodig("admin", "admin@boekenwinkel.be", "Winkel", "Beheerder", adminWachtwoord, Rol.ADMIN);
        maakAanIndienNodig("klant", "klant@boekenwinkel.be", "Jana", "Peeters", klantWachtwoord, Rol.KLANT);
    }

    private void maakAanIndienNodig(String gebruikersnaam, String email, String voornaam, String achternaam,
                                    String wachtwoord, Rol rol) {
        if (gebruikerRepository.existsByGebruikersnaamIgnoreCase(gebruikersnaam)) {
            return;
        }
        gebruikerRepository.save(new Gebruiker(gebruikersnaam, email, voornaam, achternaam,
                passwordEncoder.encode(wachtwoord), rol));
        log.info("Gebruiker '{}' aangemaakt met rol {}", gebruikersnaam, rol);
    }
}
