package app.tradeflows.api.order_service.exceptions;

import app.tradeflows.api.order_service.entities.Portfolio;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class InsufficientBalanceException extends Exception{
    private Portfolio portfolio;

    public InsufficientBalanceException(String message){
        super(message);
    }
    public InsufficientBalanceException(Portfolio portfolio){
        super();
        this.portfolio = portfolio;
    }

//    public double getCashBalance(){
//        return portfolio.getCash();
//    }

}
