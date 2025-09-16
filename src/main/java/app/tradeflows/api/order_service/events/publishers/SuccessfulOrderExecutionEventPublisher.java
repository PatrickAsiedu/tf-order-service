package app.tradeflows.api.order_service.events.publishers;

import app.tradeflows.api.order_service.dtos.NotifyExecutedOrderDTO;
import app.tradeflows.api.order_service.events.SuccessfulOrderExecutionEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SuccessfulOrderExecutionEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public SuccessfulOrderExecutionEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishEvent(List<NotifyExecutedOrderDTO> executedOrderDTOS){
        SuccessfulOrderExecutionEvent event = new SuccessfulOrderExecutionEvent(this, executedOrderDTOS);
        eventPublisher.publishEvent(event);
    }
}
