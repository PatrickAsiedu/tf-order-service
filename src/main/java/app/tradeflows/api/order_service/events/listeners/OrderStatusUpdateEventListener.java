package app.tradeflows.api.order_service.events.listeners;

import app.tradeflows.api.order_service.clients.ExchangeServerClient;

import app.tradeflows.api.order_service.dtos.exchange.CheckStatusDTO;
import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.entities.PortfolioProduct;
import app.tradeflows.api.order_service.entities.Trade;
import app.tradeflows.api.order_service.enums.OrderStatus;
import app.tradeflows.api.order_service.enums.Side;
import app.tradeflows.api.order_service.enums.TradeStatus;
import app.tradeflows.api.order_service.events.OrderStatusUpdateEvent;
import app.tradeflows.api.order_service.repositories.OrderRepository;
import app.tradeflows.api.order_service.repositories.PortfolioProductRepository;
import app.tradeflows.api.order_service.repositories.TradeRepository;
import app.tradeflows.api.order_service.events.publishers.UserAccountBalanceEventPublisher;
import app.tradeflows.api.order_service.dtos.UserBalanceUpdateDTO;
import app.tradeflows.api.order_service.enums.BalanceAction;
import app.tradeflows.api.order_service.enums.UpdateType;
import app.tradeflows.api.order_service.enums.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
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
    private final UserAccountBalanceEventPublisher userAccountBalanceEventPublisher;

    public OrderStatusUpdateEventListener(OrderRepository orderRepository, TradeRepository tradeRepository,
                                          PortfolioProductRepository portfolioProductRepository,
                                          ExchangeServerClient exchangeServerClient,
                                          UserAccountBalanceEventPublisher userAccountBalanceEventPublisher) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.portfolioProductRepository = portfolioProductRepository;
        this.exchangeServerClient = exchangeServerClient;
        this.userAccountBalanceEventPublisher = userAccountBalanceEventPublisher;
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
            Trade trade = tradeOptional.get();
            // capture previously recorded settled units to compute delta and avoid duplicate processing
            int priorSettledUnits = trade.getSettledUnit();
            exchangeServerClient.setServer(event.getExchangeServer());
            CheckStatusDTO statusDTO = exchangeServerClient.confirmOrderStatus(event.getExchangeOrderReference());
            Optional<Order> optionalOrder = orderRepository.findById(trade.getOrder().getId());
            if(optionalOrder.isEmpty()){
                logger.info("Order not found for Id {}", trade.getOrder().getId());
                return;
            }

            Order order = optionalOrder.get();
            if (statusDTO.getTradeStatus() == TradeStatus.FILLED || statusDTO.getTradeStatus() == TradeStatus.PARTIALLY_FILLED) {
                // update settled units to what's reported by exchange
                int newSettled = statusDTO.getQuantity();
                trade.setSettledUnit(newSettled);
                if (statusDTO.getTradeStatus() == TradeStatus.FILLED) {
                    trade.setDateFulfilled(LocalDateTime.now());
                    order.setStatus(hasAllRelatedTradeFilled(order, statusDTO));
                }
            }
            trade.setTradeStatus(statusDTO.getTradeStatus());
            trade.setSettledPrice(statusDTO.getPrice());
            orderRepository.save(order);
            tradeRepository.save(trade);

//            try {
//                int reported = statusDTO.getQuantity();
//                int deltaQty = Math.max(0, reported - priorSettledUnits);
//
//                if (deltaQty <= 0) {
//                    // nothing new to process
//                    return;
//                }
//
//                // create a lightweight copy of statusDTO values for delta processing
//                CheckStatusDTO deltaStatus = new CheckStatusDTO();
//                deltaStatus.setTradeStatus(statusDTO.getTradeStatus());
//                deltaStatus.setPrice(statusDTO.getPrice());
//                deltaStatus.setQuantity(deltaQty);
//
//                if (order.getType() == OrderType.MARKET ) {
//                    handleMarketOrdersBalanceUpdate(order, deltaStatus);
//                } else {
//                    handleLimitOrdersBalanceUpdate(order, deltaStatus);
//                }
//
//            } catch (Exception ex) {
//                logger.error("Failed to publish balance update event: {}", ex.getMessage());
//            }
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
public void handleMarketOrdersBalanceUpdate(Order order, CheckStatusDTO statusDTO) {
        int executedQuantity = statusDTO.getQuantity();
        double executedPrice = statusDTO.getPrice();
    if (order.getSide() == Side.BUY) {
        double priceUsedForLocking = order.getPrice();
        
        double lockedAmount = priceUsedForLocking * order.getQuantity();
        double actualCost = executedPrice * statusDTO.getQuantity();
        double difference = lockedAmount - actualCost;
        
        // 1. Remove locked funds
        UserBalanceUpdateDTO unlockFunds = new UserBalanceUpdateDTO();
        unlockFunds.setDescription("Order filled: " + statusDTO.getQuantity() + " of " + order.getProduct().getTicker());
        unlockFunds.setAmount(lockedAmount);
        unlockFunds.setAction(BalanceAction.DEBIT);
        unlockFunds.setType(UpdateType.LOCK_AMOUNT);
        unlockFunds.setUserId(order.getUserId());
        userAccountBalanceEventPublisher.publishEvent(unlockFunds);
        
        // 2. Adjust available balance if there's a difference
        if (difference != 0) {
            UserBalanceUpdateDTO adjustAvailable = new UserBalanceUpdateDTO();
            
            if (difference > 0) {
                // Refund excess (executed cheaper than expected)
                adjustAvailable.setDescription("Refund from order execution: " + statusDTO.getQuantity() + " of " + order.getProduct().getTicker());
                adjustAvailable.setAction(BalanceAction.CREDIT);
            } else {
                // Charge extra (executed more expensive than expected)
                adjustAvailable.setDescription("Additional charge from order execution: " + statusDTO.getQuantity() + " of " + order.getProduct().getTicker());
                adjustAvailable.setAction(BalanceAction.DEBIT);
            }
            
            adjustAvailable.setAmount(Math.abs(difference));
            adjustAvailable.setType(UpdateType.AVAILABLE_BALANCE);
            adjustAvailable.setUserId(order.getUserId());
            userAccountBalanceEventPublisher.publishEvent(adjustAvailable);
        }
        
  
    }else{
        double saleProceeds = executedPrice * executedQuantity;

        // Credit available balance with sale proceeds
        UserBalanceUpdateDTO creditSale = new UserBalanceUpdateDTO();
        creditSale.setDescription("Order filled: sold " + executedQuantity + " of " + order.getProduct().getTicker());
        creditSale.setAmount(saleProceeds);
        creditSale.setAction(BalanceAction.CREDIT);
        creditSale.setType(UpdateType.AVAILABLE_BALANCE);
        creditSale.setUserId(order.getUserId());
        userAccountBalanceEventPublisher.publishEvent(creditSale);
    }
}

public void handleLimitOrdersBalanceUpdate(Order order, CheckStatusDTO statusDTO) {
        logger.info("i run ,deduct balance");
          UserBalanceUpdateDTO updateDTO = new UserBalanceUpdateDTO();
            int executedQty = statusDTO.getQuantity();
            double executedPrice = statusDTO.getPrice();
            double amount = executedPrice * executedQty;

        if (order.getSide() == Side.BUY) {
            updateDTO.setAction(BalanceAction.DEBIT);
            updateDTO.setType(UpdateType.LOCK_AMOUNT);
            updateDTO.setAmount(amount);
            updateDTO.setDescription(
                    "Filled sell order: " + executedQty + " of " + order.getProduct().getTicker());
            updateDTO.setUserId(order.getUserId());
            userAccountBalanceEventPublisher.publishEvent(updateDTO);
        } else{
                // SELL: credit available balance 
                        updateDTO.setAction(BalanceAction.CREDIT);
                        updateDTO.setType(UpdateType.AVAILABLE_BALANCE);
                        updateDTO.setAmount(amount);
                        updateDTO.setDescription(
                                "Filled sell order: " + executedQty + " of " + order.getProduct().getTicker());
                        updateDTO.setUserId(order.getUserId());
                        userAccountBalanceEventPublisher.publishEvent(updateDTO);
        }
}

}
