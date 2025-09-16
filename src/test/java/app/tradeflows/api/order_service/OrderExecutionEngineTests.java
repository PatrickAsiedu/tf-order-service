package app.tradeflows.api.order_service;

import app.tradeflows.api.order_service.clients.ExchangeServerClient;
import app.tradeflows.api.order_service.config.JsonBuilder;
import app.tradeflows.api.order_service.dtos.NotifyExecutedOrderDTO;
import app.tradeflows.api.order_service.dtos.OrderDTO;
import app.tradeflows.api.order_service.dtos.SplitOrderDTO;
import app.tradeflows.api.order_service.dtos.exchange.BestOrderBook;
import app.tradeflows.api.order_service.dtos.exchange.ExecuteOrderDTO;
import app.tradeflows.api.order_service.dtos.exchange.OrderBookDTO;
import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.entities.Product;
import app.tradeflows.api.order_service.enums.ExchangeServer;
import app.tradeflows.api.order_service.enums.OrderBookFilter;
import app.tradeflows.api.order_service.enums.OrderType;
import app.tradeflows.api.order_service.enums.Side;
import app.tradeflows.api.order_service.events.publishers.SuccessfulOrderExecutionEventPublisher;
import app.tradeflows.api.order_service.services.OrderExecutionEngine;
import app.tradeflows.api.order_service.services.RedisService;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OrderExecutionEngineTests {
    @Mock
    private RedisService<Object> redisService;

    @Mock
    private ExchangeServerClient exchangeServerClient;

    @Mock
    private SuccessfulOrderExecutionEventPublisher executionEventPublisher;

    @InjectMocks
    private OrderExecutionEngine orderExecutionEngine;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Helper method to create an Order
    private Order createOrder(OrderType type, Side side, int quantity, double price) {
        Order order = new Order();
        order.setType(type);
        order.setProduct(new Product("TEST_TICKER"));
        order.setQuantity(quantity);
        order.setSide(side);
        order.setPrice(price);
        return order;
    }

    // Test handling of market orders when best offers are present
//    @Test
//    public void testProcessMarketOrderWhenBestOffersArePresent() {
//        Order order = createOrder(OrderType.MARKET, Side.BUY, 100, 50);
//
//        OrderBookDTO orderBookDTO1 = new OrderBookDTO("TEST_TICKER", 100, 10, Side.SELL, null, "",null, 0, 0);
//        OrderBookDTO orderBookDTO2 = new OrderBookDTO("TEST_TICKER", 50, 20, Side.SELL, null, "",null, 10, 0);
//
//        List<OrderBookDTO> orderBooks1 = Collections.singletonList(orderBookDTO1);
//        List<OrderBookDTO> orderBooks2 = Collections.singletonList(orderBookDTO2);
//
//        when(exchangeServerClient.getOrderBooksByProduct(order.getProduct().getTicker(), OrderBookFilter.OPEN)).thenReturn(orderBooks1);
//        when(redisService.getItem("ORDER_BOOK_TEST_TICKER_MAL2")).thenReturn(orderBooks2);
//
//        when(exchangeServerClient.executeOrder(any(ExecuteOrderDTO.class))).thenReturn("ORDER_REF");
//
//
//        orderExecutionEngine.process(order);
//
//        verify(exchangeServerClient, times(1)).executeOrder(any(ExecuteOrderDTO.class));
//        verify(executionEventPublisher, times(1)).publishEvent(anyList());
//    }

    // Test handling of limit orders with successful execution
    @Test
    public void testProcessLimitOrder() {
        Order order = createOrder(OrderType.LIMIT, Side.SELL, 100, 50);

        when(exchangeServerClient.executeOrder(any(ExecuteOrderDTO.class))).thenReturn("ORDER_REF");

        orderExecutionEngine.process(order);

        verify(exchangeServerClient, times(1)).executeOrder(any(ExecuteOrderDTO.class));
        verify(executionEventPublisher, times(1)).publishEvent(anyList());
    }

    // Test handling of market orders when the best offer list is empty
    @Test
    public void testProcessMarketOrderWhenBestOfferIsEmpty() {
        Order order = createOrder(OrderType.MARKET, Side.BUY, 100, 50);

        when(redisService.getItem("ORDER_BOOK_TEST_TICKER_MAL1")).thenReturn(List.of());
        when(redisService.getItem("ORDER_BOOK_TEST_TICKER_MAL2")).thenReturn(List.of());
        when(redisService.getItem("TEST_TICKER_BID_PRICE_MAL1")).thenReturn(10.0);
        when(redisService.getItem("TEST_TICKER_BID_PRICE_MAL2")).thenReturn(20.0);

        when(exchangeServerClient.executeOrder(any(ExecuteOrderDTO.class))).thenReturn("ORDER_REF");
        orderExecutionEngine.process(order);

        verify(exchangeServerClient, times(2)).executeOrder(any(ExecuteOrderDTO.class));
        verify(executionEventPublisher, times(1)).publishEvent(anyList());
    }

    // Test exception handling in process method
    @Test
    public void testProcessOrderWhenExceptionOccurs() {
        Order order = createOrder(OrderType.LIMIT, Side.SELL, 100, 50);

        when(exchangeServerClient.executeOrder(any(ExecuteOrderDTO.class))).thenThrow(new RuntimeException("Execution failed"));

        orderExecutionEngine.process(order);

        // No interactions should be made for event publishing due to the exception
        verify(executionEventPublisher, never()).publishEvent(anyList());
    }

    // Test handling when no filtered order book is found
    @Test
    public void testGetBestOfferFromOrderBookWhenNoFilteredOrderBook() {
        Order order = createOrder(OrderType.MARKET, Side.BUY, 100, 50);

        List<OrderBookDTO> orderBooks1 = Collections.emptyList();
        List<OrderBookDTO> orderBooks2 = Collections.emptyList();

        List<BestOrderBook> bestOffers = orderExecutionEngine.getBestOfferFromOrderBook(orderBooks1, orderBooks2, order);

        assertNull(bestOffers, "Best offers should be null when no order books are present");
    }

    // Test split order for buy order
    @Test
    public void testGetSplitOrderForBuyOrder() {
        Order order = createOrder(OrderType.MARKET, Side.BUY, 100, 50);

        when(redisService.getItem("TEST_TICKER_BID_PRICE_MAL1")).thenReturn(10.0);
        when(redisService.getItem("TEST_TICKER_BID_PRICE_MAL2")).thenReturn(20.0);

        List<SplitOrderDTO> splitOrders = orderExecutionEngine.getBestMarketOffer(order);

        assertNotNull(splitOrders, "Split orders should not be null");
        assertEquals(2, splitOrders.size(), "There should be two split orders");
    }

    // Test split order for sell order
    @Test
    public void testGetSplitOrderForSellOrder() {
        Order order = createOrder(OrderType.MARKET, Side.SELL, 100, 50);

        when(redisService.getItem("TEST_TICKER_ASK_PRICE_MAL1")).thenReturn(30.0);
        when(redisService.getItem("TEST_TICKER_ASK_PRICE_MAL2")).thenReturn(40.0);

        List<SplitOrderDTO> splitOrders = orderExecutionEngine.getBestMarketOffer(order);

        assertNotNull(splitOrders, "Split orders should not be null");
        assertEquals(2, splitOrders.size(), "There should be two split orders");
    }

}
