package app.tradeflows.api.order_service.dtos.exchange;

import app.tradeflows.api.order_service.enums.ExchangeServer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BestOrderBook {
    private ExchangeServer exchangeServer;
    private OrderBookDTO orderBookDTO;
}
