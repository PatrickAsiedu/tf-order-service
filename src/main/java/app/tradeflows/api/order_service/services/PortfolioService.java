package app.tradeflows.api.order_service.services;

import app.tradeflows.api.order_service.dtos.PortfolioDTO;
import app.tradeflows.api.order_service.entities.Portfolio;
import app.tradeflows.api.order_service.exceptions.DefaultPortfolioException;
import app.tradeflows.api.order_service.exceptions.NotFoundException;
import app.tradeflows.api.order_service.repositories.PortfolioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    // Functions takes a userid
    // Set default values for Portfolio class
    // save record
    public Portfolio createDefaultPortfolio(String userId) {
        Portfolio portfolio = new Portfolio(
                "Default",
                userId,
                true
        );
        portfolio.setCreatedAt(LocalDateTime.now());
        return portfolioRepository.save(portfolio);
    }

//    public Portfolio createPortfolio(Portfolio portfolio) {
//        return portfolioRepository.save(portfolio);
//    }
    public Portfolio createPortfolio(PortfolioDTO portfolioDTO) {
        Portfolio portfolio = new Portfolio(
                portfolioDTO.getName(),
                portfolioDTO.getUserId(),
                false
        );
        return portfolioRepository.save(portfolio);
    }

    public Portfolio getPortfolioById(String id) throws NotFoundException {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Portfolio does not exist"));
    }

    public List<Portfolio> getPortfolios() {
        return portfolioRepository.findAll();
    }

    public List<Portfolio> getPortfoliosByUserId(String id) {
        return portfolioRepository.findAllByUserId(id);
    }

    public void deletePortfolio(String id) throws NotFoundException, DefaultPortfolioException {
        Optional<Portfolio> optionalPortfolio = portfolioRepository.findById(id);
        Portfolio portfolio = optionalPortfolio.orElseThrow(() -> new NotFoundException("Order does not exist"));

        if (portfolio.isDefault())
            throw new DefaultPortfolioException("Portfolio is user's default");

        portfolioRepository.delete(portfolio);
    }

    public PortfolioDTO updatePortfolio(PortfolioDTO portfolioDTO, String id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Portfolio does not exist"));

        portfolio.setName(portfolioDTO.getName());
        portfolioRepository.save(portfolio);

        return portfolioDTO;
    }
}
