package app.tradeflows.api.order_service.dtos.exchange;

import app.tradeflows.api.order_service.enums.OrderType;
import app.tradeflows.api.order_service.enums.Side;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OrderBookDTO(
        @JsonProperty(value = "product")
        String ticker,
        int quantity,
        double price,
        Side side,
        List<ExecutionsDTO> executions,
        @JsonProperty(value = "orderID")
        String orderId,
        OrderType orderType,
        @JsonProperty(value = "cumulatitiveQuantity")
        int cumulativeQuantity,
        @JsonProperty(value = "cumulatitivePrice")
        double cumulativePrice
) {
}