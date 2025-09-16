package app.tradeflows.api.order_service.dtos;

import app.tradeflows.api.order_service.enums.ExchangeServer;
import app.tradeflows.api.order_service.enums.OrderType;
import app.tradeflows.api.order_service.enums.Side;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WebhookDTO {
    private OrderType orderType;
    private String product;
    private Side side;
    private String orderID;
    private double price;
    private int qty;
    private int cumQty;
    private double cumPrx;
    private ExchangeServer exchange;
    private LocalDateTime timestamp;
}
