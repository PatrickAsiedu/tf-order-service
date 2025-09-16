package app.tradeflows.api.order_service.controllers;

import app.tradeflows.api.order_service.dtos.PortfolioDTO;
import app.tradeflows.api.order_service.entities.Portfolio;
import app.tradeflows.api.order_service.entities.PortfolioProduct;
import app.tradeflows.api.order_service.exceptions.DefaultPortfolioException;
import app.tradeflows.api.order_service.exceptions.NotFoundException;
import app.tradeflows.api.order_service.repositories.PortfolioRepository;
import app.tradeflows.api.order_service.services.PortfolioProductService;
import app.tradeflows.api.order_service.services.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/portfolios")
@RestController
public class PortfolioController {
    @Autowired
    PortfolioProductService portfolioProductService;

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Portfolio> createPortfolio(@RequestBody PortfolioDTO portfolioDTO) {
        List<Portfolio> portfolios = portfolioService.getPortfoliosByUserId(portfolioDTO.getUserId());

        Portfolio result;
        if (portfolios.isEmpty()) {
            result = portfolioService.createDefaultPortfolio(portfolioDTO.getUserId());
        }
        else {
            result = portfolioService.createPortfolio(portfolioDTO);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Portfolio> getPortfolioById(@PathVariable(value = "id") String id) throws NotFoundException {
        Portfolio result = portfolioService.getPortfolioById(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping()
    public ResponseEntity<List<Portfolio>> getPortfolios() {
        List<Portfolio> portfolios = portfolioService.getPortfolios();
        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Portfolio>> getPortfoliosByUserId(@PathVariable(value = "userId") String userId) {
        List<Portfolio> portfolios = portfolioService.getPortfoliosByUserId(userId);
        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<List<PortfolioProduct>> getPortfoliosProducts(String id) {
        List<PortfolioProduct> portfolioProducts = portfolioProductService.getPortfolioProductsByPortfolioId(id);
        return ResponseEntity.ok(portfolioProducts);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PortfolioDTO> updatePortfolio(@RequestBody PortfolioDTO portfolioDTO, @PathVariable(value = "id") String id) {
        PortfolioDTO result = portfolioService.updatePortfolio(portfolioDTO, id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public void deletePortfolio(@PathVariable(value = "id") String id) throws DefaultPortfolioException {
        portfolioService.deletePortfolio(id);
    }
}
