package app.tradeflows.api.order_service;

import app.tradeflows.api.order_service.entities.PortfolioProduct;
import app.tradeflows.api.order_service.exceptions.NotFoundException;
import app.tradeflows.api.order_service.repositories.PortfolioProductRepository;
import app.tradeflows.api.order_service.services.PortfolioProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortfolioProductServiceTests {

    @Mock
    PortfolioProductRepository portfolioProductRepository;

    @InjectMocks
    PortfolioProductService portfolioProductService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        portfolioProductService = new PortfolioProductService(portfolioProductRepository);
    }

    @Test
    void testCreatePortfolioProductIsSuccessful() {
        PortfolioProduct portfolioProduct = new PortfolioProduct();
        when(portfolioProductService.createPortfolioProduct(portfolioProduct)).thenReturn(portfolioProduct);

        PortfolioProduct result = portfolioProductService.createPortfolioProduct(portfolioProduct);

        assertEquals(portfolioProduct, result);
    }

    @Test
    public void testCreatePortfolioProductNotSuccessful() {
        /*PortfolioProduct portfolioProduct = new PortfolioProduct();
        when(portfolioProductService.createPortfolioProduct(portfolioProduct)).thenReturn(portfolioProduct);

        PortfolioProduct result = portfolioProductService.createPortfolioProduct(portfolioProduct);
        assertEquals(result, portfolioProduct);*/
        PortfolioProduct portfolioProduct = new PortfolioProduct();

        when(portfolioProductRepository.save(portfolioProduct)).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            portfolioProductService.createPortfolioProduct(portfolioProduct);
        });
    }

    @Test
    public void testGetPortfolioProductByIdReturnsPortfolio() throws NotFoundException {
        PortfolioProduct portfolioProduct = new PortfolioProduct();
        portfolioProduct.setId("portfolioProduct123");

        when(portfolioProductRepository.findById("portfolioProduct123")).thenReturn(Optional.of(portfolioProduct));

        PortfolioProduct portfolioResult = portfolioProductService.getPortfolioProductById("portfolioProduct123");

        assertEquals("portfolioProduct123", portfolioResult.getId());
        verify(portfolioProductRepository, times(1)).findById("portfolioProduct123");
    }

    @Test
    public void testGetPortfolioProductByIdNotSuccessful() throws NotFoundException {
        Exception exception = assertThrows(NotFoundException.class, () -> {
            portfolioProductService.getPortfolioProductById("portfolioProduct123");
        });
        System.out.println(exception.getMessage());
        String expectedMessage = "Portfolio product does not exist";
        assertTrue(exception.getMessage().contains(expectedMessage));
    }

    @Test
    void testGetPortfolioProductsReturnsNonEmptyResult() {
        List<PortfolioProduct> portfolioProducts = Arrays.asList(new PortfolioProduct(), new PortfolioProduct());
        when(portfolioProductRepository.findAll()).thenReturn(portfolioProducts);

        List<PortfolioProduct> portfolioProductsResult = portfolioProductService.getPortfolioProducts();
        assertEquals(2, portfolioProductsResult.size());
        verify(portfolioProductRepository, times(1)).findAll();
    }

    @Test
    void testGetPortfolioProductsReturnsEmptyResult() {
        List<PortfolioProduct> portfolios = List.of();
        when(portfolioProductRepository.findAll()).thenReturn(portfolios);

        List<PortfolioProduct> portfolioProductsResult = portfolioProductService.getPortfolioProducts();
        assertEquals(0, portfolioProductsResult.size());
        verify(portfolioProductRepository, times(1)).findAll();
    }

}
