package app.tradeflows.api.order_service.events;

import app.tradeflows.api.order_service.enums.ExchangeServer;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderStatusUpdateEvent extends ApplicationEvent {
    private final ExchangeServer exchangeServer;
    private final String exchangeOrderReference;

    public OrderStatusUpdateEvent(Object source, ExchangeServer exchangeServer, String exchangeOrderReference) {
        super(source);
        this.exchangeServer = exchangeServer;
        this.exchangeOrderReference = exchangeOrderReference;
    }
}
