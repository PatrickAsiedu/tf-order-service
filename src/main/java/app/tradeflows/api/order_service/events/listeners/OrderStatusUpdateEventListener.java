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

            if (tradeOptional.isEmpty()) {
                logger.info("Trade not found for Reference {}", event.getExchangeOrderReference());
                return;
            }

            Trade trade = tradeOptional.get();

            // Capture previously recorded settled units to compute delta and avoid duplicate processing
            int priorSettledUnits = trade.getSettledUnit();

            exchangeServerClient.setServer(event.getExchangeServer());
            CheckStatusDTO statusDTO = exchangeServerClient.confirmOrderStatus(event.getExchangeOrderReference());

            Optional<Order> optionalOrder = orderRepository.findById(trade.getOrder().getId());
            if (optionalOrder.isEmpty()) {
                logger.info("Order not found for Id {}", trade.getOrder().getId());
                return;
            }

            Order order = optionalOrder.get();

            // Calculate NEW settled units (delta) - only process what's new
            int newlySettledUnits = statusDTO.getQuantity() - priorSettledUnits;

            if (newlySettledUnits <= 0) {
                logger.info("No new units settled for trade {} (prior: {}, current: {})",
                        trade.getId(), priorSettledUnits, statusDTO.getQuantity());
                return; // Nothing new to process, avoid duplicate updates
            }

            logger.info("Processing {} newly settled units for trade {} (prior: {}, current: {})",
                    newlySettledUnits, trade.getId(), priorSettledUnits, statusDTO.getQuantity());

            // Update trade with latest info from exchange
            trade.setSettledUnit(statusDTO.getQuantity());
            trade.setTradeStatus(statusDTO.getTradeStatus());
            trade.setSettledPrice(statusDTO.getPrice());

            if (statusDTO.getTradeStatus() == TradeStatus.FILLED) {
                trade.setDateFulfilled(LocalDateTime.now());
            }

            // Update portfolio with ONLY the newly settled units
            managePortfolio(order, statusDTO, newlySettledUnits);

            // Calculate overall order status based on all related trades
            order.setStatus(calculateOrderStatus(order));

            orderRepository.save(order);
            tradeRepository.save(trade);

            // Optionally handle balance updates based on order type
            // Uncomment and adjust as needed for your balance logic

            try {
                CheckStatusDTO deltaStatus = new CheckStatusDTO();
                deltaStatus.setTradeStatus(statusDTO.getTradeStatus());
                deltaStatus.setPrice(statusDTO.getPrice());
                deltaStatus.setQuantity(newlySettledUnits);

                if (order.getType() == OrderType.MARKET) {
                    handleMarketOrdersBalanceUpdate(order, deltaStatus);
                } else {
                    handleLimitOrdersBalanceUpdate(order, deltaStatus);
                }
            } catch (Exception ex) {
                logger.error("Failed to publish balance update event: {}", ex.getMessage(), ex);
            }


        } catch (Exception ex) {
            logger.error("An error occurred processing order status update for reference {}: {}",
                    event.getExchangeOrderReference(), ex.getMessage(), ex);
        }
    }

    /**
     * Calculate the overall order status based on all related trades
     */
    private OrderStatus calculateOrderStatus(Order order) {
        var trades = tradeRepository.findByOrder_Id(order.getId());

        int totalFilledUnits = trades.stream()
                .filter(trade -> trade.getTradeStatus() == TradeStatus.FILLED ||
                        trade.getTradeStatus() == TradeStatus.PARTIALLY_FILLED)
                .mapToInt(Trade::getSettledUnit)
                .sum();

        if (totalFilledUnits <= 0) {
            return OrderStatus.OPEN;
        } else if (totalFilledUnits < order.getQuantity()) {
            return OrderStatus.PARTIALLY_FILLED;
        } else {
            return OrderStatus.FILLED;
        }
    }

    public void managePortfolio(Order order, CheckStatusDTO statusDTO, int newlySettledUnits) {
        PortfolioProduct portfolioProduct = portfolioProductRepository
                .findByPortfolioIdAndProductId(order.getPortfolio().getId(), order.getProduct().getId());

        double executionPrice = statusDTO.getPrice();
        double executionValue = newlySettledUnits * executionPrice;

        if (Objects.isNull(portfolioProduct)) {
            // Create new portfolio entry for first-time holding
            portfolioProduct = new PortfolioProduct();
            portfolioProduct.setPortfolio(order.getPortfolio());
            portfolioProduct.setProduct(order.getProduct());
            portfolioProduct.setQuantity(newlySettledUnits);
            portfolioProduct.setPrice(executionPrice);
            portfolioProduct.setValue(executionValue);
            portfolioProduct.setCreatedAt(LocalDateTime.now());

            logger.info("Created new portfolio product: {} units of {} at price {}",
                    newlySettledUnits, order.getProduct().getTicker(), executionPrice);
        } else {
            if (order.getSide() == Side.BUY) {
                // BUY: Add to position with weighted average price
                double oldValue = portfolioProduct.getValue();
                int oldQuantity = portfolioProduct.getQuantity();

                double newValue = oldValue + executionValue;
                int newQuantity = oldQuantity + newlySettledUnits;

                portfolioProduct.setQuantity(newQuantity);
                portfolioProduct.setValue(newValue);
                portfolioProduct.setPrice(newValue / newQuantity); // Weighted average cost

                logger.info("Updated BUY: added {} units at {} (old: {} @ {}, new: {} @ {})",
                        newlySettledUnits, executionPrice, oldQuantity,
                        portfolioProduct.getPrice(), newQuantity, newValue / newQuantity);
            } else {
                // SELL: Reduce position
                int oldQuantity = portfolioProduct.getQuantity();
                int oldLockedQuantity = portfolioProduct.getLockedQuantity();
                double oldValue = portfolioProduct.getValue();

                portfolioProduct.setLockedQuantity(oldLockedQuantity - newlySettledUnits);
                // Remove proportional value based on average cost
                double avgCost = oldValue / (oldQuantity + oldLockedQuantity);
                portfolioProduct.setValue(oldValue - (avgCost * newlySettledUnits));

                // Price (average cost) stays the same unless all shares are gone
                if ((oldQuantity + portfolioProduct.getLockedQuantity()) <= 0) {
                    portfolioProduct.setPrice(0.0);
                }
                // Otherwise price remains unchanged (same avg cost basis)

                logger.info("Updated SELL: unlocked {} units at {} (available: {}, locked: {} -> {}, value: {} -> {})",
                        newlySettledUnits, executionPrice, oldQuantity,
                        oldLockedQuantity, portfolioProduct.getLockedQuantity(),
                        oldValue, portfolioProduct.getValue());
            }
        }

        portfolioProductRepository.save(portfolioProduct);
    }

    public void handleMarketOrdersBalanceUpdate(Order order, CheckStatusDTO statusDTO) {
        int executedQuantity = statusDTO.getQuantity();
        double executedPrice = statusDTO.getPrice();

        if (order.getSide() == Side.BUY) {
            double priceUsedForLocking = order.getPrice();

            double lockedAmount = priceUsedForLocking * executedQuantity;
            double actualCost = executedPrice * executedQuantity;
            double difference = lockedAmount - actualCost;

            // 1. Remove locked funds
            UserBalanceUpdateDTO unlockFunds = new UserBalanceUpdateDTO();
            unlockFunds.setDescription("Order filled: " + executedQuantity + " of " + order.getProduct().getTicker());
            unlockFunds.setAmount(lockedAmount);
            unlockFunds.setAction(BalanceAction.DEBIT);
            unlockFunds.setType(UpdateType.LOCK_AMOUNT);
            unlockFunds.setUserId(order.getUserId());
            userAccountBalanceEventPublisher.publishEvent(unlockFunds);

            // 2. Adjust available balance if there's a difference
            if (Math.abs(difference) > 0.01) { // Use small epsilon for double comparison
                UserBalanceUpdateDTO adjustAvailable = new UserBalanceUpdateDTO();

                if (difference > 0) {
                    // Refund excess (executed cheaper than expected)
                    adjustAvailable.setDescription("Refund from order execution: " + executedQuantity + " of " + order.getProduct().getTicker());
                    adjustAvailable.setAction(BalanceAction.CREDIT);
                } else {
                    // Charge extra (executed more expensive than expected)
                    adjustAvailable.setDescription("Additional charge from order execution: " + executedQuantity + " of " + order.getProduct().getTicker());
                    adjustAvailable.setAction(BalanceAction.DEBIT);
                }

                adjustAvailable.setAmount(Math.abs(difference));
                adjustAvailable.setType(UpdateType.AVAILABLE_BALANCE);
                adjustAvailable.setUserId(order.getUserId());
                userAccountBalanceEventPublisher.publishEvent(adjustAvailable);
            }
        } else {
            // SELL side
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
        logger.info("Processing limit order balance update");

        UserBalanceUpdateDTO updateDTO = new UserBalanceUpdateDTO();
        int executedQty = statusDTO.getQuantity();
        double executedPrice = statusDTO.getPrice();
        double amount = executedPrice * executedQty;

        if (order.getSide() == Side.BUY) {
            updateDTO.setAction(BalanceAction.DEBIT);
            updateDTO.setType(UpdateType.LOCK_AMOUNT);
            updateDTO.setAmount(amount);
            updateDTO.setDescription("Filled buy order: " + executedQty + " of " + order.getProduct().getTicker());
            updateDTO.setUserId(order.getUserId());
            userAccountBalanceEventPublisher.publishEvent(updateDTO);
        } else {
            // SELL: credit available balance
            updateDTO.setAction(BalanceAction.CREDIT);
            updateDTO.setType(UpdateType.AVAILABLE_BALANCE);
            updateDTO.setAmount(amount);
            updateDTO.setDescription("Filled sell order: " + executedQty + " of " + order.getProduct().getTicker());
            updateDTO.setUserId(order.getUserId());
            userAccountBalanceEventPublisher.publishEvent(updateDTO);
        }
    }
}