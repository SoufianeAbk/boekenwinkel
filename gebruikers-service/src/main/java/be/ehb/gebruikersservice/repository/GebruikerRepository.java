package be.ehb.gebruikersservice.repository;

import be.ehb.gebruikersservice.model.Gebruiker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GebruikerRepository extends JpaRepository<Gebruiker, Long> {

    Optional<Gebruiker> findByGebruikersnaamIgnoreCase(String gebruikersnaam);

    boolean existsByGebruikersnaamIgnoreCase(String gebruikersnaam);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
