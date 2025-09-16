package app.tradeflows.api.order_service;

import app.tradeflows.api.order_service.dtos.OrderDTO;
import app.tradeflows.api.order_service.dtos.OrderDTOGet;
import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.entities.Portfolio;
import app.tradeflows.api.order_service.entities.Product;
import app.tradeflows.api.order_service.enums.OrderStatus;
import app.tradeflows.api.order_service.enums.OrderType;
import app.tradeflows.api.order_service.enums.Side;
import app.tradeflows.api.order_service.events.publishers.UserAccountBalanceEventPublisher;
import app.tradeflows.api.order_service.exceptions.InsufficientBalanceException;
import app.tradeflows.api.order_service.exceptions.InsufficientStocksException;
import app.tradeflows.api.order_service.exceptions.InvalidOrderException;
import app.tradeflows.api.order_service.exceptions.NotFoundException;
import app.tradeflows.api.order_service.repositories.*;
import app.tradeflows.api.order_service.services.OrderService;
import app.tradeflows.api.order_service.services.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
public class OrderServiceTests {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PortfolioProductRepository portfolioProductRepository;
    @Mock
    private RedisService<Object> redisService;
    @Mock
    private UserAccountBalanceEventPublisher userAccountBalanceEventPublisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

//    @Test
//    void testSendOrder() {
//        OrderDTO orderDTO = new OrderDTO();
//        ResponseEntity<String> responseEntity = ResponseEntity.ok("Success");
//        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(responseEntity);
//
//        ResponseEntity<String> response = orderService.sendOrder(orderDTO);
//        assertEquals(responseEntity, response);
//        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
//    }

//    @Test
//    void testCreateOrder_SuccessfulBuyOrder() throws Exception {
//        OrderDTO orderDTO = new OrderDTO();
//        orderDTO.setSide(Side.BUY);
//        orderDTO.setUserId("user1");
//        orderDTO.setQuantity(10);
//        orderDTO.setPrice(100);
//        orderDTO.setProduct("product1");
//        orderDTO.setPortfolioId("portfolio1");
//
//        Portfolio portfolio = new Portfolio();
//        Product product = new Product();
//        product.setBidPrice(100);
//        product.setMaxShiftPrice(10);
//        product.setBuyLimit(100);
//        product.setSellLimit(100);
//
//        when(portfolioRepository.findById(anyString())).thenReturn(Optional.of(portfolio));
//        when(productRepository.findByTicker(anyString())).thenReturn(Optional.of(product));
//        when(redisService.getItem(anyString())).thenReturn("{\"availableBalance\":2000}");
//
//        Order order = new Order();
//        when(orderRepository.save(any(Order.class))).thenReturn(order);
//
//        Order createdOrder = orderService.createOrder(orderDTO);
//        assertNotNull(createdOrder);
//        verify(userAccountBalanceEventPublisher).publishEvent(any());
//    }

//    @Test
//    void testCreateOrder_Failure_InsufficientBalance() throws Exception {
//        OrderDTO orderDTO = new OrderDTO();
//        orderDTO.setSide(Side.BUY);
//        orderDTO.setUserId("user1");
//        orderDTO.setQuantity(10);
//        orderDTO.setPrice(100);
//        orderDTO.setProduct("product1");
//        orderDTO.setPortfolioId("portfolio1");
//
//        Product product = new Product();
//        product.setBidPrice(100);
//        product.setMaxShiftPrice(10);
//        product.setBuyLimit(100);
//        product.setSellLimit(100);
//
//        when(productRepository.findByTicker(anyString())).thenReturn(Optional.of(product));
//        when(redisService.getItem(anyString())).thenReturn("{\"availableBalance\":500}");
//
//        assertThrows(InsufficientBalanceException.class, () -> {
//            orderService.createOrder(orderDTO);
//        });
//    }

//    @Test
//    void testGetOrders() {
//        orderService.getOrders();
//        verify(orderRepository).findAll();
//    }

//    @Test
//    void testGetOrdersByUserIdAndStatus() {
//        String userId = "user1";
//        String status = "PENDING";
//        orderService.getOrdersByUserIdAndStatus(userId, status);
//        verify(orderRepository).findByUserIdAndStatus(userId, OrderStatus.valueOf(status.toUpperCase()));
//    }

//    @Test
//    void testGetOrderById() throws NotFoundException {
//        Order order = new Order();
//        when(orderRepository.findById(anyString())).thenReturn(Optional.of(order));
//
//        Order foundOrder = orderService.getOrderById("order1");
//        assertNotNull(foundOrder);
//    }

//    @Test
//    void testGetOrderById_NotFound() {
//        when(orderRepository.findById(anyString())).thenReturn(Optional.empty());
//
//        assertThrows(NotFoundException.class, () -> {
//            orderService.getOrderById("order1");
//        });
//    }

//    @Test
//    void testGetOrderFromExchangeById() {
//        String orderRef = "orderRef1";
//        OrderDTOGet orderDTOGet = new OrderDTOGet();
//        when(restTemplate.getForEntity(anyString(), eq(OrderDTOGet.class))).thenReturn(ResponseEntity.ok(orderDTOGet));
//
//        OrderDTOGet result = orderService.getOrderFromExchangeById(orderRef);
//        assertNotNull(result);
//    }

//    @Test
//    void testCancelOrder() throws NotFoundException {
//        Order order = new Order();
//        order.setStatus(OrderStatus.PENDING);
//        when(orderRepository.findById(anyString())).thenReturn(Optional.of(order));
//        when(orderRepository.save(any(Order.class))).thenReturn(order);
//
//        boolean result = orderService.cancelOrder("order1");
//        assertTrue(result);
//        assertEquals(OrderStatus.CANCELLED, order.getStatus());
//        verify(orderRepository).save(any(Order.class));
//    }

//    @Test
//    void testCancelOrder_NotFound() {
//        when(orderRepository.findById(anyString())).thenReturn(Optional.empty());
//
//        assertThrows(NotFoundException.class, () -> {
//            orderService.cancelOrder("order1");
//        });
//    }
}
