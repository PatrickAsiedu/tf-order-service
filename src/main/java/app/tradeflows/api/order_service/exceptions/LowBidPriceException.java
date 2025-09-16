package app.tradeflows.api.order_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LowBidPriceException extends Exception {
    public LowBidPriceException(String message) {
        super(message);
    }
}
