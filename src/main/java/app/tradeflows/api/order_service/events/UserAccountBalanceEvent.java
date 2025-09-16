package app.tradeflows.api.order_service.events;

import app.tradeflows.api.order_service.dtos.UserBalanceUpdateDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserAccountBalanceEvent extends ApplicationEvent {
    private final UserBalanceUpdateDTO updateDTO;

    public UserAccountBalanceEvent(Object source, UserBalanceUpdateDTO updateDTO) {
        super(source);
        this.updateDTO = updateDTO;
    }
}
