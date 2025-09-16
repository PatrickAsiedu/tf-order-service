package app.tradeflows.api.order_service.exceptions;

public class ForbiddenException extends Exception{
    public ForbiddenException(String message) {
        super(message);
    }
}
