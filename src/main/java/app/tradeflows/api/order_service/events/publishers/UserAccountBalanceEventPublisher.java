package app.tradeflows.api.order_service.events.publishers;

import app.tradeflows.api.order_service.dtos.NotifyExecutedOrderDTO;
import app.tradeflows.api.order_service.dtos.UserBalanceUpdateDTO;
import app.tradeflows.api.order_service.events.SuccessfulOrderExecutionEvent;
import app.tradeflows.api.order_service.events.UserAccountBalanceEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserAccountBalanceEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public UserAccountBalanceEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishEvent(UserBalanceUpdateDTO updateDTO){
        UserAccountBalanceEvent event = new UserAccountBalanceEvent(this, updateDTO);
        eventPublisher.publishEvent(event);
    }
}
