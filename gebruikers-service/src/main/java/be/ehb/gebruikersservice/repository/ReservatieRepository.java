package be.ehb.gebruikersservice.repository;

import be.ehb.gebruikersservice.model.Reservatie;
import be.ehb.gebruikersservice.model.ReservatieStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservatieRepository extends JpaRepository<Reservatie, Long> {

    List<Reservatie> findByGebruikerIdOrderByAangemaaktOpDesc(Long gebruikerId);

    Optional<Reservatie> findByIdAndGebruikerId(Long id, Long gebruikerId);

    List<Reservatie> findByStatusOrderByAangemaaktOpAsc(ReservatieStatus status);

    List<Reservatie> findAllByOrderByAangemaaktOpDesc();
}
