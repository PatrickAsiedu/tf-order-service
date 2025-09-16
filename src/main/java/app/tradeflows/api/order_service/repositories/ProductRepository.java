package app.tradeflows.api.order_service.repositories;

import app.tradeflows.api.order_service.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findByTicker(String ticker);
}
