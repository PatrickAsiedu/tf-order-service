package app.tradeflows.api.order_service.dtos;

import app.tradeflows.api.order_service.enums.OrderType;
import app.tradeflows.api.order_service.enums.Side;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class OrderDTOGet {
    private String product;
    private int quantity;
    private double price;
    @Enumerated(EnumType.STRING)
    @NotNull
    private Side side;
    @Enumerated(EnumType.STRING)
    @NotNull
    private OrderType orderType;

    private int cumulativeQuantity;
}
