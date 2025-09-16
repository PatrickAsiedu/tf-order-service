package app.tradeflows.api.order_service.dtos.exchange;

import app.tradeflows.api.order_service.enums.TradeStatus;
import lombok.Data;

@Data
public class CheckStatusDTO {
    private TradeStatus tradeStatus;
    private double price;
    private int quantity;
}
