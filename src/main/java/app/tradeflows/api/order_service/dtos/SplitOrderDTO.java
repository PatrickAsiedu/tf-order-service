package app.tradeflows.api.order_service.dtos;

import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.enums.ExchangeServer;
import lombok.Data;

@Data
public class SplitOrderDTO {
    private int quantity;
    private double price;
    private ExchangeServer exchangeServer;
}
