package app.tradeflows.api.order_service.dtos;

import app.tradeflows.api.order_service.enums.OrderType;
import app.tradeflows.api.order_service.enums.Side;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class OrderDTO {
    @NotNull
    private String product;
    @Min(1)
    @NotNull
    private int quantity;
    @NotNull
    private double price;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Side side;
    @NotNull
    @Enumerated(EnumType.STRING)
    private OrderType type;

    @NotNull
    private String userId;
    @NotNull
    private String portfolioId;
}

//{
//        "product":"SUNW",
//        "quantity":2,
//        "price": 0.11,
//        "side":"BUY",
//        “type”:”LIMIT”,
//        }