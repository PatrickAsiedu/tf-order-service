package app.tradeflows.api.order_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class InsufficientStocksException extends Exception{

    public InsufficientStocksException(String message){
        super(message);
    }



}
