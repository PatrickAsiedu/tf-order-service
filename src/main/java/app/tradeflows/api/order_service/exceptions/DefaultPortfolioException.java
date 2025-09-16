package app.tradeflows.api.order_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class DefaultPortfolioException extends Exception{
    public DefaultPortfolioException(String message) {
        super(message);
    }
}
