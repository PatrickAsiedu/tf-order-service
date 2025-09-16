package app.tradeflows.api.order_service.dtos;

import app.tradeflows.api.order_service.enums.ExchangeServer;
import lombok.Data;

@Data
public class NotifyExecutedOrderDTO {
    private String orderId;
    private String userId;
    private int quantity;
    private double price;
    private String exchangeServerReference;
    private ExchangeServer exchangeServer;
}
