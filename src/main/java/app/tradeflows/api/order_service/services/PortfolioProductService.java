package app.tradeflows.api.order_service.services;

import app.tradeflows.api.order_service.entities.PortfolioProduct;
import app.tradeflows.api.order_service.exceptions.NotFoundException;
import app.tradeflows.api.order_service.repositories.PortfolioProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PortfolioProductService {
    PortfolioProductRepository portfolioProductRepository;

    public PortfolioProductService(PortfolioProductRepository portfolioProductRepository) {
        this.portfolioProductRepository = portfolioProductRepository;
    }

    public PortfolioProduct createPortfolioProduct(PortfolioProduct portfolioProduct) {
        portfolioProduct.setCreatedAt(LocalDateTime.now());
        return portfolioProductRepository.save(portfolioProduct);
    }

    public PortfolioProduct getPortfolioProductById(String id) throws NotFoundException {
        return portfolioProductRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Portfolio product does not exist"));
    }

    public List<PortfolioProduct> getPortfolioProducts() {
        return portfolioProductRepository.findAll();
    }

    public List<PortfolioProduct> getPortfolioProductsByPortfolioId(String id) {
        return portfolioProductRepository.findByPortfolioId(id);
    }
}
