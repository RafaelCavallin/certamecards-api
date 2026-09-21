package br.com.certamecards.events.persistence;

import br.com.certamecards.events.domain.ProductEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductEventRepository extends JpaRepository<ProductEvent, UUID> {}
