package app.tradeflows.api.order_service.repositories;

import app.tradeflows.api.order_service.entities.PortfolioProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioProductRepository extends JpaRepository<PortfolioProduct, String> {
    List<PortfolioProduct> findByPortfolioId(String id);

    PortfolioProduct findByPortfolioIdAndProductId(String portfolioId, String productId);

}
