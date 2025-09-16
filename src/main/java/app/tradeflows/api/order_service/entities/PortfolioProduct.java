package app.tradeflows.api.order_service.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Table(name = "tf_portfolio_products")
@Entity
@Setter
@Getter
public class PortfolioProduct extends Audit {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    private String id;

    @JsonIgnore
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
    private double value;

    private int lockedQuantity;

}
