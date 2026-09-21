package br.com.certamecards.review.persistence;

import br.com.certamecards.review.domain.ReviewVoid;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewVoidRepository extends JpaRepository<ReviewVoid, UUID> {}
