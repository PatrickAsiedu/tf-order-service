package app.tradeflows.api.order_service.dtos.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductDTO(
        @JsonProperty(value = "LAST_TRADED_PRICE")
        double lastTradedPrice,
        @JsonProperty(value = "TICKER")
        String ticker,
        @JsonProperty(value = "SELL_LIMIT")
        int sellLimit,
        @JsonProperty(value = "BID_PRICE")
        double bidPrice,
        @JsonProperty(value = "BUY_LIMIT")
        int buyLimit,
        @JsonProperty(value = "ASK_PRICE")
        double askPrice,
        @JsonProperty(value = "MAX_PRICE_SHIFT")
        double maxPriceShift
) {
}
