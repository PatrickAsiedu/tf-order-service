package app.tradeflows.api.order_service.exceptions;

public class InvalidOrderException extends Exception{
    public InvalidOrderException(String message) {
        super(message);
    }
}
