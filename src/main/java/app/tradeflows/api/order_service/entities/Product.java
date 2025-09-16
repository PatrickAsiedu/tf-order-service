package app.tradeflows.api.order_service.entities;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "tf_products")
@Entity
@Setter
@Getter
@NoArgsConstructor
public class Product extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(unique = true, nullable = false)
    private String ticker;
    private int buyLimit;
    private int sellLimit;
    private double lastTradedPrice;
    private double askPrice;
    private double bidPrice;
    private double maxShiftPrice;
    private boolean isTrading = true;

    public Product(
            String ticker, int buyLimit, int sellLimit, double lastTradedPrice,
            double askPrice, double bidPrice, double maxShiftPrice
    ) {
        this.ticker = ticker;
        this.buyLimit = buyLimit;
        this.sellLimit = sellLimit;
        this.lastTradedPrice = lastTradedPrice;
        this.askPrice = askPrice;
        this.bidPrice = bidPrice;
        this.maxShiftPrice = maxShiftPrice;
    }

    public Product(String ticker) {

        this.ticker = ticker;
    }
}
