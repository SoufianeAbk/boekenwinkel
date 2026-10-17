package be.ehb.catalogusservice.repository;

import be.ehb.catalogusservice.dto.ReviewStatistiek;
import be.ehb.catalogusservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByBoekIsbnOrderByAangemaaktOpDesc(String isbn);

    Optional<Review> findByBoekIsbnAndGebruikerId(String isbn, Long gebruikerId);

    @Query("""
            select new be.ehb.catalogusservice.dto.ReviewStatistiek(r.boek.isbn, avg(r.score), count(r))
            from Review r group by r.boek.isbn
            """)
    List<ReviewStatistiek> statistiekenPerBoek();
}
