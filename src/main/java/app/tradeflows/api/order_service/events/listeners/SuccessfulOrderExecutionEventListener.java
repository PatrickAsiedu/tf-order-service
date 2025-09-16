package app.tradeflows.api.order_service.events.listeners;

import app.tradeflows.api.order_service.config.JsonBuilder;
import app.tradeflows.api.order_service.config.KafkaProperties;
import app.tradeflows.api.order_service.dtos.NotifyExecutedOrderDTO;
import app.tradeflows.api.order_service.dtos.UserBalanceUpdateDTO;
import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.entities.Trade;
import app.tradeflows.api.order_service.enums.*;
import app.tradeflows.api.order_service.events.SuccessfulOrderExecutionEvent;
import app.tradeflows.api.order_service.events.publishers.AuditLogEventPublisher;
import app.tradeflows.api.order_service.events.publishers.UserAccountBalanceEventPublisher;
import app.tradeflows.api.order_service.repositories.OrderRepository;
import app.tradeflows.api.order_service.repositories.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class SuccessfulOrderExecutionEventListener {
    private static final Logger logger = LoggerFactory.getLogger(SuccessfulOrderExecutionEventListener.class);
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final AuditLogEventPublisher auditLogEventPublisher;
    private final UserAccountBalanceEventPublisher userAccountBalanceEventPublisher;

    public SuccessfulOrderExecutionEventListener(KafkaTemplate<String, String> kafkaTemplate, KafkaProperties properties,
                                                 OrderRepository orderRepository, TradeRepository tradeRepository,
                                                 AuditLogEventPublisher auditLogEventPublisher, UserAccountBalanceEventPublisher userAccountBalanceEventPublisher) {
        this.userAccountBalanceEventPublisher = userAccountBalanceEventPublisher;
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.auditLogEventPublisher = auditLogEventPublisher;
    }

    @Transactional
    @EventListener
    public void handleSuccessfulOrderExecutionEvent(SuccessfulOrderExecutionEvent event) {
        for (NotifyExecutedOrderDTO executedOrder : event.getExecutedOrderDTOS()) {
            processExecutedOrder(executedOrder);
        }
    }

    private void processExecutedOrder(NotifyExecutedOrderDTO executedOrder) {
        Optional<Order> optionalOrder = orderRepository.findById(executedOrder.getOrderId());
        if (optionalOrder.isEmpty()) {
            logger.info("Unable to find order for {}", executedOrder.getOrderId());
            return;
        }

        Order order = optionalOrder.get();
        updateOrderStatus(order);
        createTrade(executedOrder, order);
        sendUserBalanceUpdate(executedOrder, order);
        publishAuditLog(executedOrder, order);
    }

    private void updateOrderStatus(Order order) {
        order.setStatus(OrderStatus.OPEN);
        orderRepository.save(order);
    }

    private void createTrade(NotifyExecutedOrderDTO executedOrder, Order order) {
        Trade trade = new Trade();
        trade.setOrder(order);
        trade.setExchangeServerReference(executedOrder.getExchangeServerReference());
        trade.setExchangeServerId(executedOrder.getExchangeServer().getType());
        trade.setTradeStatus(TradeStatus.PENDING);
        trade.setSettledUnit(executedOrder.getQuantity());
        trade.setSettledPrice(executedOrder.getPrice());
        trade.setCreatedAt(LocalDateTime.now());
        trade.setDateFulfilled(LocalDateTime.now());
        tradeRepository.save(trade);
    }

    private void sendUserBalanceUpdate(NotifyExecutedOrderDTO executedOrder, Order order) {
        UserBalanceUpdateDTO updateDTO = createUserBalanceUpdateDTO(executedOrder, order);
        userAccountBalanceEventPublisher.publishEvent(updateDTO);
    }

    private void publishAuditLog(NotifyExecutedOrderDTO executedOrder, Order order) {
        String actionDescription = String.format("%s %d units of %s for %.2f",
                order.getSide(), executedOrder.getQuantity(), order.getProduct().getTicker(), executedOrder.getPrice());
        auditLogEventPublisher.publishLogEvent(
                executedOrder.getUserId(),
                "ORDER EXECUTION",
                actionDescription,
                "USER",
                null
        );
    }

    private UserBalanceUpdateDTO createUserBalanceUpdateDTO(NotifyExecutedOrderDTO executedOrder, Order order) {
        UserBalanceUpdateDTO updateDTO = new UserBalanceUpdateDTO();
        updateDTO.setAction(order.getSide() == Side.BUY ? BalanceAction.DEBIT : BalanceAction.CREDIT);
        updateDTO.setType(UpdateType.LOCK_AMOUNT);
        updateDTO.setUserId(executedOrder.getUserId());
        updateDTO.setAmount(executedOrder.getPrice());
        updateDTO.setDescription(String.format("%s %d units of %s", order.getSide(), executedOrder.getQuantity(), order.getProduct().getTicker()));
        return updateDTO;
    }
}
