package app.tradeflows.api.order_service.dtos.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ExecutionsDTO (
        LocalDateTime timestamp,
        double price,
        int quantity
) {
}


