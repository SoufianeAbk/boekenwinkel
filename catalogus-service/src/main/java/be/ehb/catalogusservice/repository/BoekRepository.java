package be.ehb.catalogusservice.repository;

import be.ehb.catalogusservice.model.Boek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoekRepository extends JpaRepository<Boek, String> {

    List<Boek> findAllByOrderByTitelAsc();

    @Query("""
            select distinct b from Boek b left join b.auteurs a
            where lower(b.titel) like lower(concat('%', :zoekterm, '%'))
               or lower(a) like lower(concat('%', :zoekterm, '%'))
               or b.isbn like concat('%', :zoekterm, '%')
            order by b.titel
            """)
    List<Boek> zoek(@Param("zoekterm") String zoekterm);
}
