package app.tradeflows.api.order_service.events;

import app.tradeflows.api.order_service.dtos.NotifyExecutedOrderDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class SuccessfulOrderExecutionEvent extends ApplicationEvent {
    private final List<NotifyExecutedOrderDTO> executedOrderDTOS;

    public SuccessfulOrderExecutionEvent(Object source, List<NotifyExecutedOrderDTO> executedOrderDTOS) {
        super(source);
        this.executedOrderDTOS = executedOrderDTOS;
    }
}
