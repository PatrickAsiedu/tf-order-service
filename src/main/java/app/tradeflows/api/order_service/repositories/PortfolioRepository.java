package app.tradeflows.api.order_service.repositories;

import app.tradeflows.api.order_service.entities.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {
    List<Portfolio> findAllByUserId(String userId);
}
