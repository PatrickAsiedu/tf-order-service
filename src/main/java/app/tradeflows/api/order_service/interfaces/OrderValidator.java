package app.tradeflows.api.order_service.interfaces;

import app.tradeflows.api.order_service.dtos.OrderDTO;

public interface OrderValidator {
    boolean priceIsValid(OrderDTO orderDTO);
    boolean quantityIsValid(OrderDTO orderDTO);
}
