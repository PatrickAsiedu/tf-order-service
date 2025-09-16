package app.tradeflows.api.order_service;

import app.tradeflows.api.order_service.dtos.PortfolioDTO;
import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.entities.Portfolio;
import app.tradeflows.api.order_service.exceptions.DefaultPortfolioException;
import app.tradeflows.api.order_service.exceptions.NotFoundException;
import app.tradeflows.api.order_service.repositories.PortfolioRepository;
import app.tradeflows.api.order_service.services.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortfolioServiceTest {

    @Mock
    PortfolioRepository portfolioRepository;

    @InjectMocks
    PortfolioService portfolioService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        portfolioService = new PortfolioService(portfolioRepository);
    }

    @Test
    public void testCreatePortfolioIsSuccessful() {
        PortfolioDTO portfolioDTO = new PortfolioDTO();
        portfolioDTO.setName("Health");
        portfolioDTO.setUserId("user123");

        Portfolio portfolio = new Portfolio("Tech", "user123", false);

        when(portfolioService.createPortfolio(portfolioDTO)).thenReturn(portfolio);
//        when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
        Portfolio result = portfolioService.createPortfolio(portfolioDTO);
//        Portfolio result = portfolioService.createPortfolio(portfolio);

        assertEquals(portfolio, result);
    }

    @Test
    public void testJpaSavePortfolioIsSuccessful() {
        PortfolioDTO portfolioDTO = new PortfolioDTO();
        portfolioDTO.setName("Health");
        portfolioDTO.setUserId("user123");
        Portfolio portfolio = new Portfolio(portfolioDTO.getName(), "user123", false);

        when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
//        when(portfolioService.createPortfolio(portfolio)).thenReturn(portfolio);
        Portfolio result = portfolioService.createPortfolio(portfolioDTO);
//        Portfolio result = portfolioService.createPortfolio(portfolio);
        assertEquals(portfolio, result);
    }

    @Test
    public void testCreatePortfolioNotSuccessful() {
        Portfolio portfolio = new Portfolio();
        PortfolioDTO portfolioDTO = new PortfolioDTO();

        when(portfolioRepository.save(portfolio)).thenThrow(new RuntimeException("Save failed"));

        assertThrows(RuntimeException.class, () -> {
            portfolioService.createPortfolio(portfolioDTO);
//            portfolioService.createPortfolio(portfolio);
        });
    }

    @Test
    public void testGetPortfolioByIdReturnsPortfolio() throws NotFoundException {
        Portfolio portfolio = new Portfolio();
        portfolio.setId("portfolio123");

        when(portfolioRepository.findById("portfolio123")).thenReturn(Optional.of(portfolio));

        Portfolio portfolioResult = portfolioService.getPortfolioById("portfolio123");

        assertEquals("portfolio123", portfolioResult.getId());
        verify(portfolioRepository, times(1)).findById("portfolio123");
    }

    @Test
    public void testGetPortfolioByIdNotSuccessful() throws NotFoundException {
        Exception exception = assertThrows(NotFoundException.class, () -> {
            portfolioService.getPortfolioById("portfolio123");
        });

        String expectedMessage = "Portfolio does not exist";
        assertTrue(exception.getMessage().contains(expectedMessage));
    }

    @Test
    void testGetPortfoliosReturnsNonEmptyResult() {
        List<Portfolio> portfolios = Arrays.asList(new Portfolio(), new Portfolio());
        when(portfolioRepository.findAll()).thenReturn(portfolios);

        List<Portfolio> portfoliosResult = portfolioService.getPortfolios();
        assertEquals(2, portfoliosResult.size());
        verify(portfolioRepository, times(1)).findAll();
    }

    @Test
    void testGetPortfoliosReturnsEmptyResult() {
        List<Portfolio> portfolios = List.of();
        when(portfolioRepository.findAll()).thenReturn(portfolios);

        List<Portfolio> portfoliosResult = portfolioService.getPortfolios();
        assertEquals(0, portfoliosResult.size());
        verify(portfolioRepository, times(1)).findAll();
    }

    @Test
    void testGetPortfoliosByUserIdReturnsNonEmptyResult() {
        List<Portfolio> portfolios = Arrays.asList(new Portfolio(), new Portfolio());
        when(portfolioRepository.findAllByUserId("user1")).thenReturn(portfolios);

        List<Portfolio> portfoliosResult = portfolioService.getPortfoliosByUserId("user1");
        assertEquals(2, portfoliosResult.size());
        verify(portfolioRepository, times(1)).findAllByUserId("user1");
    }

    @Test
    void testGetPortfoliosByUserIdReturnsEmptyResult() {
        List<Portfolio> portfolios = List.of();
        when(portfolioRepository.findAllByUserId("user1")).thenReturn(portfolios);

        List<Portfolio> portfoliosResult = portfolioService.getPortfoliosByUserId("user1");
        assertEquals(0, portfoliosResult.size());
        verify(portfolioRepository, times(1)).findAllByUserId("user1");
    }

    @Test
    void testDeleteNonDefaultPortfolioIsSuccessful() throws NotFoundException, DefaultPortfolioException {
        Portfolio portfolio = new Portfolio("Tech", "user123", false);
        when(portfolioRepository.findById("portfolio123")).thenReturn(Optional.of(portfolio));

        portfolioService.deletePortfolio("portfolio123");
    }

    @Test
    void testDeleteDefaultPortfolioIsNotSuccessful() {
        Portfolio portfolio = new Portfolio("Tech", "user123", true);
        when(portfolioRepository.findById("portfolio123")).thenReturn(Optional.of(portfolio));

        Exception exception = assertThrows(DefaultPortfolioException.class, () -> {
            portfolioService.deletePortfolio("portfolio123");
        });

        String expected = "Portfolio is user's default";
        assertTrue(exception.getMessage().contains(expected));
    }
}
