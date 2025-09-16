package app.tradeflows.api.order_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Table(name = "tf_portfolios")
@Entity
@Setter
@Getter
@NoArgsConstructor
public class Portfolio extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private String id;

    @Column(nullable = false)
    private String name;

//    @Column(nullable = false)
//    @JoinColumn(name = "user_id")
    @Column(nullable = false)
    private String userId;

    private boolean isDefault;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "portfolio_id")
    private List<PortfolioProduct> products;

    public Portfolio(String name, String userId, boolean isDefault) {
        this.name = name;
        this.userId = userId;
        this.isDefault = isDefault;
    }

}
