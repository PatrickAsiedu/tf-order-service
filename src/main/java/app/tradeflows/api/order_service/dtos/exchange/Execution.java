package app.tradeflows.api.order_service.dtos.exchange;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Execution {
    private LocalDateTime timestamp;
    private double price;
    private int quantity;

    // Constructor
    public Execution(LocalDateTime timestamp, double price, int quantity) {
        this.timestamp = timestamp;
        this.price = price;
        this.quantity = quantity;
    }

}

