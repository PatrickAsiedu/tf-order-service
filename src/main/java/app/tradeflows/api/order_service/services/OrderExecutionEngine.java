package app.tradeflows.api.order_service.services;

import app.tradeflows.api.order_service.clients.ExchangeServerClient;
import app.tradeflows.api.order_service.config.JsonBuilder;
import app.tradeflows.api.order_service.dtos.NotifyExecutedOrderDTO;
import app.tradeflows.api.order_service.dtos.SplitOrderDTO;
import app.tradeflows.api.order_service.dtos.exchange.BestOrderBook;
import app.tradeflows.api.order_service.dtos.exchange.ExecuteOrderDTO;
import app.tradeflows.api.order_service.dtos.exchange.OrderBookDTO;
import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.enums.ExchangeServer;
import app.tradeflows.api.order_service.enums.OrderBookFilter;
import app.tradeflows.api.order_service.enums.OrderType;
import app.tradeflows.api.order_service.enums.Side;
import app.tradeflows.api.order_service.events.publishers.SuccessfulOrderExecutionEventPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.aspectj.weaver.ast.Or;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OrderExecutionEngine {

    private static final Logger logger = LoggerFactory.getLogger(OrderExecutionEngine.class);
    private final RedisService<Object> redisService;
    private final ExchangeServerClient exchangeServerClient;
    private final SuccessfulOrderExecutionEventPublisher executionEventPublisher;

    public OrderExecutionEngine(RedisService<Object> redisService, ExchangeServerClient exchangeServerClient, SuccessfulOrderExecutionEventPublisher executionEventPublisher) {
        this.redisService = redisService;
        this.exchangeServerClient = exchangeServerClient;
        this.executionEventPublisher = executionEventPublisher;
    }


    @Transactional
    @Async
    public void process(Order order) {
        try {
            ExecuteOrderDTO orderDTO = createOrderDTO(order);
            List<NotifyExecutedOrderDTO> executedOrderDTOS = new ArrayList<>();

            if (order.getType() == OrderType.MARKET) {
                List<OrderBookDTO> orderBooks = getOrderBooksFromCache(order.getProduct().getTicker(), ExchangeServer.MAL1);
                List<OrderBookDTO> orderBooks2 = getOrderBooksFromCache(order.getProduct().getTicker(), ExchangeServer.MAL2);

                List<BestOrderBook> bestOffers = getBestOfferFromOrderBook(orderBooks, orderBooks2, order);

                if (bestOffers == null || bestOffers.isEmpty()) {
                    handleMarketOrder(order, orderDTO, executedOrderDTOS);
                } else {
                    handleLimitOrder(order, orderDTO, bestOffers, executedOrderDTOS);
                }

                executionEventPublisher.publishEvent(executedOrderDTOS);
            } else {
                handleLimitOrderPlacement(order, orderDTO, executedOrderDTOS);
                executionEventPublisher.publishEvent(executedOrderDTOS);
            }
        } catch (Exception exception) {
            logger.info("An error occurred while executing order, {}", exception.getMessage());
        }
    }

    private ExecuteOrderDTO createOrderDTO(Order order) {
        ExecuteOrderDTO orderDTO = new ExecuteOrderDTO();
        orderDTO.setProduct(order.getProduct().getTicker());
        orderDTO.setQuantity(order.getQuantity());
        orderDTO.setSide(order.getSide());
        orderDTO.setPrice(order.getPrice());
        return orderDTO;
    }

    private List<OrderBookDTO> getOrderBooksFromCache(String ticker, ExchangeServer exchangeServer) {
        exchangeServerClient.setServer(exchangeServer);
        return exchangeServerClient.getOrderBooksByProduct(ticker, OrderBookFilter.OPEN);
    }

    private void handleMarketOrder(Order order, ExecuteOrderDTO orderDTO, List<NotifyExecutedOrderDTO> executedOrderDTOS) {
        List<SplitOrderDTO> splitOrderDTOList = getBestMarketOffer(order);

        splitOrderDTOList.forEach(bestOffer -> {
            orderDTO.setType(OrderType.MARKET);
            orderDTO.setQuantity(bestOffer.getQuantity());
            exchangeServerClient.setServer(bestOffer.getExchangeServer());

            String orderReference = exchangeServerClient.executeOrder(orderDTO);
            logger.info("orderReference: {}", orderReference);
            NotifyExecutedOrderDTO executedOrder = createExecutedOrderDTO(order, orderDTO, bestOffer.getExchangeServer(), orderReference);
            executedOrderDTOS.add(executedOrder);
        });
    }

    private void handleLimitOrder(Order order, ExecuteOrderDTO orderDTO, List<BestOrderBook> bestOffers, List<NotifyExecutedOrderDTO> executedOrderDTOS) {
        bestOffers.forEach(bestOffer -> {
            orderDTO.setType(OrderType.LIMIT);
            orderDTO.setQuantity(order.getQuantity());
            orderDTO.setPrice(bestOffer.getOrderBookDTO().price() < 0 ? order.getPrice() : bestOffer.getOrderBookDTO().price());

            exchangeServerClient.setServer(bestOffer.getExchangeServer());
            String orderReference = exchangeServerClient.executeOrder(orderDTO);
            logger.info("orderReference: {}", orderReference);
            NotifyExecutedOrderDTO executedOrder = createExecutedOrderDTO(order, bestOffer, bestOffer.getExchangeServer(), orderReference);
            executedOrderDTOS.add(executedOrder);
        });
    }

    private void handleLimitOrderPlacement(Order order, ExecuteOrderDTO orderDTO, List<NotifyExecutedOrderDTO> executedOrderDTOS) {
        orderDTO.setType(order.getType());
        exchangeServerClient.setServer(ExchangeServer.MAL1);
        String orderReference = exchangeServerClient.executeOrder(orderDTO);

        NotifyExecutedOrderDTO executedOrder = createExecutedOrderDTO(order, orderDTO, ExchangeServer.MAL1, orderReference);
        executedOrderDTOS.add(executedOrder);
    }

    private NotifyExecutedOrderDTO createExecutedOrderDTO(Order order, ExecuteOrderDTO orderDTO, ExchangeServer exchangeServer, String orderReference) {
        NotifyExecutedOrderDTO executedOrder = new NotifyExecutedOrderDTO();
        executedOrder.setExchangeServer(exchangeServer);
        executedOrder.setQuantity(orderDTO.getQuantity());
        executedOrder.setOrderId(order.getId());
        executedOrder.setUserId(order.getUserId());
        executedOrder.setExchangeServerReference(orderReference);
        executedOrder.setPrice(orderDTO.getPrice());
        return executedOrder;
    }

    private NotifyExecutedOrderDTO createExecutedOrderDTO(Order order, BestOrderBook orderDTO, ExchangeServer exchangeServer, String orderReference) {
        NotifyExecutedOrderDTO executedOrder = new NotifyExecutedOrderDTO();
        executedOrder.setExchangeServer(exchangeServer);
        executedOrder.setQuantity(orderDTO.getOrderBookDTO().quantity());
        executedOrder.setOrderId(order.getId());
        executedOrder.setUserId(order.getUserId());
        executedOrder.setExchangeServerReference(orderReference);
        executedOrder.setPrice(orderDTO.getOrderBookDTO().price());
        return executedOrder;
    }
    public List<BestOrderBook> getBestOfferFromOrderBook(List<OrderBookDTO> orderBooks, List<OrderBookDTO> orderBooks2, Order order) {
        if (orderBooks.isEmpty() && orderBooks2.isEmpty()) return null;

        BestOrderBook bestOrder = new BestOrderBook();

        boolean isBuy = order.getSide() == Side.BUY;
        Optional<OrderBookDTO> filteredOrderBook = getFilteredOrderBook(orderBooks, order, isBuy);
        Optional<OrderBookDTO> filteredOrderBook2 = getFilteredOrderBook(orderBooks2, order, isBuy);

        if (filteredOrderBook.isEmpty() && filteredOrderBook2.isEmpty()) {
            return handleEmptyFilteredOrderBooks(orderBooks, orderBooks2, order, isBuy);
        }

        if (filteredOrderBook.isPresent() && filteredOrderBook2.isPresent()) {
            return compareFilteredOrderBooks(filteredOrderBook, filteredOrderBook2, isBuy, bestOrder);
        }

        if (filteredOrderBook.isPresent()) {
            bestOrder.setOrderBookDTO(filteredOrderBook.get());
            bestOrder.setExchangeServer(ExchangeServer.MAL1);
            return List.of(bestOrder);
        }

        bestOrder.setOrderBookDTO(filteredOrderBook2.get());
        bestOrder.setExchangeServer(ExchangeServer.MAL2);
        return List.of(bestOrder);
    }

    private Optional<OrderBookDTO> getFilteredOrderBook(List<OrderBookDTO> orderBooks, Order order, boolean isBuy) {
        try{
            return orderBooks.stream()
                    .filter(item -> item.side() == (isBuy ? Side.SELL : Side.BUY))
                    .filter(item -> item.quantity() - item.cumulativeQuantity() >= order.getQuantity())
                    .min(isBuy ? Comparator.comparingDouble(OrderBookDTO::price) : Comparator.comparingDouble(OrderBookDTO::price).reversed());
        }catch(Exception exception){
            logger.error(exception.toString(), exception);
            return Optional.empty();
        }
    }

    private List<BestOrderBook> handleEmptyFilteredOrderBooks(List<OrderBookDTO> orderBooks, List<OrderBookDTO> orderBooks2, Order order, boolean isBuy) {
        List<BestOrderBook> selected = new ArrayList<>();
        Optional<OrderBookDTO> bestOrderBook = orderBooks.stream()
                .filter(item -> item.side() == (isBuy ? Side.SELL : Side.BUY))
                .min(isBuy ? Comparator.comparingDouble(OrderBookDTO::price) : Comparator.comparingDouble(OrderBookDTO::price).reversed());

        Optional<OrderBookDTO> bestOrderBook2 = orderBooks2.stream()
                .filter(item -> item.side() == (isBuy ? Side.SELL : Side.BUY))
                .min(isBuy ? Comparator.comparingDouble(OrderBookDTO::price) : Comparator.comparingDouble(OrderBookDTO::price).reversed());

        if (bestOrderBook.isEmpty() || bestOrderBook2.isEmpty()) return null;

        int quantityLeft;
        OrderBookDTO selectedBestOrderBook = bestOrderBook.get();
        if ((selectedBestOrderBook.quantity() - selectedBestOrderBook.cumulativeQuantity()) <= order.getQuantity()
                && (selectedBestOrderBook.quantity() - selectedBestOrderBook.cumulativeQuantity()) > 0) {
            bestOrderBook = Optional.of(selectedBestOrderBook);
            selected.add(createBestOrderBook(bestOrderBook.get(), ExchangeServer.MAL1));
            quantityLeft = selectedBestOrderBook.quantity() - (selectedBestOrderBook.quantity() - selectedBestOrderBook.cumulativeQuantity());
        } else {
            quantityLeft = 0;
        }

        if (quantityLeft > 0) {
            selectedBestOrderBook = bestOrderBook2.get();
            if ((selectedBestOrderBook.quantity() - selectedBestOrderBook.cumulativeQuantity()) <= quantityLeft
                    && (selectedBestOrderBook.quantity() - selectedBestOrderBook.cumulativeQuantity()) > 0) {
                bestOrderBook = Optional.of(selectedBestOrderBook);
                selected.add(createBestOrderBook(bestOrderBook.get(), ExchangeServer.MAL2));
                return selected;
            }

            Optional<OrderBookDTO> remainingOrderBook = orderBooks2.stream()
                    .filter(item -> item.side() == (isBuy ? Side.SELL : Side.BUY))
                    .filter(book -> (book.quantity() - book.cumulativeQuantity()) <= quantityLeft
                            && (book.quantity() - book.cumulativeQuantity()) > 0)
                    .findFirst();

            remainingOrderBook.ifPresent(orderBookDTO -> selected.add(createBestOrderBook(orderBookDTO, ExchangeServer.MAL2)));
        }

        return selected;
    }

    private List<BestOrderBook> compareFilteredOrderBooks(Optional<OrderBookDTO> filteredOrderBook, Optional<OrderBookDTO> filteredOrderBook2, boolean isBuy, BestOrderBook bestOrder) {
        if (filteredOrderBook.get().price() < filteredOrderBook2.get().price()) {
            bestOrder.setOrderBookDTO(filteredOrderBook.get());
            bestOrder.setExchangeServer(ExchangeServer.MAL1);
            return List.of(bestOrder);
        }

        bestOrder.setOrderBookDTO(filteredOrderBook2.get());
        bestOrder.setExchangeServer(ExchangeServer.MAL2);
        return List.of(bestOrder);
    }

    private BestOrderBook createBestOrderBook(OrderBookDTO orderBookDTO, ExchangeServer exchangeServer) {
        BestOrderBook bestOrderBook = new BestOrderBook();
        bestOrderBook.setOrderBookDTO(orderBookDTO);
        bestOrderBook.setExchangeServer(exchangeServer);
        return bestOrderBook;
    }

    public List<SplitOrderDTO> getBestMarketOffer(Order order) {
        return switch (order.getSide()) {
            case BUY -> getSplitOrderForBuyOrder(order);
            case SELL -> getSplitOrderForSellOrder(order);
        };
    }

    private List<SplitOrderDTO> getSplitOrderForBuyOrder(Order order) {
        logger.info("Getting the bid prices from redis");
        double bidPrice1 = fetchPriceFromCache(order.getProduct().getTicker()+"_BID_PRICE_MAL1");
        double bidPrice2 = fetchPriceFromCache(order.getProduct().getTicker()+"_BID_PRICE_MAL2");

        logger.info("BID_PRICE_MAL1: {}", bidPrice1);
        logger.info("BID_PRICE_MAL2: {}", bidPrice2);

        return splitOrder(order, bidPrice1, bidPrice2);
    }

    private List<SplitOrderDTO> getSplitOrderForSellOrder(Order order) {
        logger.info("Getting the ask prices from redis");
        double askPrice1 = fetchPriceFromCache(order.getProduct().getTicker()+"_ASK_PRICE_MAL1");
        double askPrice2 = fetchPriceFromCache(order.getProduct().getTicker()+"_ASK_PRICE_MAL2");

        logger.info("ASK_PRICE_MAL1: {}", askPrice1);
        logger.info("ASK_PRICE_MAL2: {}", askPrice2);

        return splitOrder(order, askPrice1, askPrice2);
    }

    private double fetchPriceFromCache(String key) {
        logger.info("key: {}", key);
        return (double)redisService.getItem(key);
    }

    private List<SplitOrderDTO> splitOrder(Order order, double price1, double price2) {
        List<SplitOrderDTO> splitOrderDTOList = new ArrayList<>();
        boolean isFirstPriceLower = price1 < price2;

        logPriceComparison(isFirstPriceLower);

        if(order.getQuantity() <= 3){
            addSplitOrderDTO(splitOrderDTOList, ExchangeServer.MAL1, order.getQuantity(), isFirstPriceLower ? price1 : price2);
            return splitOrderDTOList;
        }

        int quantityFirstOrder = (int) Math.floor(order.getQuantity() * 0.6);
        int quantitySecondOrder = order.getQuantity() - quantityFirstOrder;

        addSplitOrderDTO(splitOrderDTOList, ExchangeServer.MAL1, quantityFirstOrder, isFirstPriceLower ? price1 : price2);
        addSplitOrderDTO(splitOrderDTOList, ExchangeServer.MAL2, quantitySecondOrder, isFirstPriceLower ? price2 : price1);

        return splitOrderDTOList;
    }

    private void logPriceComparison(boolean isFirstPriceLower) {
        if (isFirstPriceLower) {
            logger.info("First price is lower than the second price");
        } else {
            logger.info("Second price is lower than the first price");
        }
    }

    private void addSplitOrderDTO(List<SplitOrderDTO> list, ExchangeServer exchangeServer, int quantity, double price) {
        SplitOrderDTO splitOrderDTO = new SplitOrderDTO();
        splitOrderDTO.setExchangeServer(exchangeServer);
        splitOrderDTO.setQuantity(quantity);
        splitOrderDTO.setPrice(price);
        list.add(splitOrderDTO);
    }

}
