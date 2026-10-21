package be.ehb.gebruikersservice.repository;

import be.ehb.gebruikersservice.model.Melding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeldingRepository extends JpaRepository<Melding, Long> {

    List<Melding> findByGebruikerIdOrderByAangemaaktOpDesc(Long gebruikerId);

    @Modifying
    @Query("update Melding m set m.gelezen = true where m.gebruiker.id = :gebruikerId and m.gelezen = false")
    int markeerAllesGelezen(@Param("gebruikerId") Long gebruikerId);
}
