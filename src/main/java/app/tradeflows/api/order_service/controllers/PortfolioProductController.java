package app.tradeflows.api.order_service.controllers;

import app.tradeflows.api.order_service.entities.PortfolioProduct;
import app.tradeflows.api.order_service.exceptions.NotFoundException;
import app.tradeflows.api.order_service.services.PortfolioProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/portfolio-products")
@RestController
public class PortfolioProductController {

    PortfolioProductService portfolioProductService;

    public PortfolioProductController(PortfolioProductService portfolioProductService) {
        this.portfolioProductService = portfolioProductService;
    }

    @PostMapping("/create")
    public String createPortfolioProduct(@RequestBody PortfolioProduct portfolioProduct) {
        return portfolioProductService.createPortfolioProduct(portfolioProduct).getId();
    }

    @GetMapping("/")
    public ResponseEntity<List<PortfolioProduct>> getPortfolioProducts() throws NotFoundException {
        List<PortfolioProduct> portfolioProducts = portfolioProductService.getPortfolioProducts();
        return ResponseEntity.ok(portfolioProducts);
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<PortfolioProduct> getPortfolioProductById(@PathVariable(value = "id") String id) throws NotFoundException {
//        PortfolioProduct portfolioProduct = portfolioProductService.getPortfolioProductById(id);
//        return ResponseEntity.ok(portfolioProduct);
//    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<List<PortfolioProduct>> getPortfolioProductsByPortfolioId(@PathVariable String portfolioId) {
        List<PortfolioProduct> portfolioProducts = portfolioProductService.getPortfolioProductsByPortfolioId(portfolioId);
        return ResponseEntity.ok(portfolioProducts);
    }
}
