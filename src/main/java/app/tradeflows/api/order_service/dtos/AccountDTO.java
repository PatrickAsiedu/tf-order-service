package app.tradeflows.api.order_service.dtos;

import lombok.Data;

@Data
public class AccountDTO {
    private String id;
    private UserDTO user;
    private double availableBalance;
    private double lockedAmount;
    private boolean isActive;
}
