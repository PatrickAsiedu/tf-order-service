package app.tradeflows.api.order_service.entities;

import app.tradeflows.api.order_service.enums.OrderStatus;
import app.tradeflows.api.order_service.enums.OrderType;
import app.tradeflows.api.order_service.enums.Side;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "tf_orders")
@Entity
@Setter
@Getter
@NoArgsConstructor
public class Order extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private String id;

    @JoinColumn(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Side side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    private String description; // reason for failure/cancelling

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    public Order(Portfolio portfolio, String userId, Product product, int quantity, double price, Side side, OrderType type) {
        this.portfolio = portfolio;
        this.userId = userId;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.type = type;
    }

}
