package app.tradeflows.api.order_service.events.listeners;

import app.tradeflows.api.order_service.clients.ExchangeServerClient;
import app.tradeflows.api.order_service.config.JsonBuilder;
import app.tradeflows.api.order_service.config.KafkaProperties;
import app.tradeflows.api.order_service.dtos.exchange.CheckStatusDTO;
import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.entities.PortfolioProduct;
import app.tradeflows.api.order_service.entities.Trade;
import app.tradeflows.api.order_service.enums.OrderStatus;
import app.tradeflows.api.order_service.enums.Side;
import app.tradeflows.api.order_service.enums.TradeStatus;
import app.tradeflows.api.order_service.events.OrderStatusUpdateEvent;
import app.tradeflows.api.order_service.events.UserAccountBalanceEvent;
import app.tradeflows.api.order_service.repositories.OrderRepository;
import app.tradeflows.api.order_service.repositories.PortfolioProductRepository;
import app.tradeflows.api.order_service.repositories.TradeRepository;
import app.tradeflows.api.order_service.services.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Component
public class OrderStatusUpdateEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderStatusUpdateEventListener.class);
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final PortfolioProductRepository portfolioProductRepository;
    private final ExchangeServerClient exchangeServerClient;

    public OrderStatusUpdateEventListener(OrderRepository orderRepository, TradeRepository tradeRepository,
                                          PortfolioProductRepository portfolioProductRepository,
                                          ExchangeServerClient exchangeServerClient) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.portfolioProductRepository = portfolioProductRepository;
        this.exchangeServerClient = exchangeServerClient;
    }


    @Transactional
    @EventListener
    public void handleAuditLogEvent(OrderStatusUpdateEvent event) {

        try {
            Optional<Trade> tradeOptional = tradeRepository.findByExchangeServerReference(event.getExchangeOrderReference());

            if(tradeOptional.isEmpty()){
                logger.info("Trade not found for Reference{}", event.getExchangeOrderReference());
                return;
            }
            Trade trade = tradeOptional.get();;
            exchangeServerClient.setServer(event.getExchangeServer());
            CheckStatusDTO statusDTO = exchangeServerClient.confirmOrderStatus(event.getExchangeOrderReference());
            Optional<Order> optionalOrder = orderRepository.findById(trade.getOrder().getId());
            if(optionalOrder.isEmpty()){
                logger.info("Order not found for Id {}", trade.getOrder().getId());
                return;
            }

            Order order = optionalOrder.get();
            if (statusDTO.getTradeStatus() == TradeStatus.FILLED) {
                trade.setDateFulfilled(LocalDateTime.now());
                trade.setSettledUnit(statusDTO.getQuantity());
                order.setStatus(hasAllRelatedTradeFilled(order, statusDTO));
            }
            trade.setTradeStatus(statusDTO.getTradeStatus());
            trade.setSettledPrice(statusDTO.getPrice());
            orderRepository.save(order);
            tradeRepository.save(trade);
        }catch (Exception ex){
            logger.error("An error occurred publishing to topic, {}", ex.getMessage());
        }
    }

    public OrderStatus hasAllRelatedTradeFilled(Order order, CheckStatusDTO statusDTO){
        var trades = tradeRepository.findByOrder_Id(order.getId());
        int totalFilledTrades = trades.stream().filter(trade -> Objects.equals(trade.getTradeStatus(), TradeStatus.FILLED)).mapToInt(Trade::getSettledUnit).sum();
        if(totalFilledTrades <= 0){
            return OrderStatus.OPEN;
        }

        if(totalFilledTrades < order.getQuantity()){
            return OrderStatus.PARTIALLY_FILLED;
        }

        managePortfolio(order, statusDTO);
        return OrderStatus.FILLED;
    }

    public void managePortfolio(Order order, CheckStatusDTO statusDTO){
        PortfolioProduct portfolioProduct = portfolioProductRepository.findByPortfolioIdAndProductId(order.getPortfolio().getId(), order.getProduct().getId());
        if(Objects.isNull(portfolioProduct)){
            portfolioProduct = new PortfolioProduct();
            portfolioProduct.setPortfolio(order.getPortfolio());
            portfolioProduct.setProduct(order.getProduct());
            portfolioProduct.setQuantity(order.getQuantity());
            portfolioProduct.setPrice(statusDTO.getPrice());
            portfolioProduct.setValue(order.getQuantity() * statusDTO.getPrice());
            portfolioProduct.setCreatedAt(LocalDateTime.now());
            portfolioProductRepository.save(portfolioProduct);
            return;
        }
        if(order.getSide() == Side.BUY){
            portfolioProduct.setPortfolio(order.getPortfolio());
            portfolioProduct.setProduct(order.getProduct());
            portfolioProduct.setQuantity(portfolioProduct.getQuantity() + order.getQuantity());
            portfolioProduct.setPrice(statusDTO.getPrice());
            portfolioProduct.setValue(portfolioProduct.getValue() + (order.getQuantity() * statusDTO.getPrice()));
            portfolioProductRepository.save(portfolioProduct);
            return;
        }
        portfolioProduct.setPortfolio(order.getPortfolio());
        portfolioProduct.setProduct(order.getProduct());
        portfolioProduct.setLockedQuantity(portfolioProduct.getLockedQuantity() - order.getQuantity());
        portfolioProduct.setPrice(statusDTO.getPrice());
        portfolioProduct.setValue(portfolioProduct.getValue() - (order.getQuantity() * statusDTO.getPrice()));
        portfolioProductRepository.save(portfolioProduct);
    }
}
