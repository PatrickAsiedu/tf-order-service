package app.tradeflows.api.order_service.dtos.exchange;

import app.tradeflows.api.order_service.enums.Side;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class OrderBook {
    private int quantity;
    private double price;
    private Side side;
    private List<Execution> executions;
    private String orderType;
    private String product;
    private String orderID;
    private int cumulatitiveQuantity;
    private double cumulatitivePrice;

    // Constructor
    public OrderBook(int quantity, double price, Side side, List<Execution> executions,
                 String orderType, String product, String orderID, int cumulatitiveQuantity,
                 double cumulatitivePrice) {
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.executions = executions;
        this.orderType = orderType;
        this.product = product;
        this.orderID = orderID;
        this.cumulatitiveQuantity = cumulatitiveQuantity;
        this.cumulatitivePrice = cumulatitivePrice;
    }

}
