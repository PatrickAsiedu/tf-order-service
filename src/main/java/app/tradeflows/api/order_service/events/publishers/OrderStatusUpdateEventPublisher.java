package app.tradeflows.api.order_service.events.publishers;

import app.tradeflows.api.order_service.dtos.UserBalanceUpdateDTO;
import app.tradeflows.api.order_service.enums.ExchangeServer;
import app.tradeflows.api.order_service.events.OrderStatusUpdateEvent;
import app.tradeflows.api.order_service.events.UserAccountBalanceEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusUpdateEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public OrderStatusUpdateEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishEvent(ExchangeServer exchangeServer, String exchangeOrderReference){
        OrderStatusUpdateEvent event = new OrderStatusUpdateEvent(this, exchangeServer, exchangeOrderReference);
        eventPublisher.publishEvent(event);
    }
}
