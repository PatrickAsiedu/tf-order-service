package app.tradeflows.api.order_service.entities;

import app.tradeflows.api.order_service.enums.TradeStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Table(name = "tf_trades")
@Setter
@Getter
@Entity
public class Trade extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    private int settledUnit;
    private double settledPrice;
    @NotNull
    private String exchangeServerId;
    @NotNull
    private String exchangeServerReference;
    @Enumerated(EnumType.STRING)
    private TradeStatus tradeStatus;
    private LocalDateTime dateFulfilled;
    private LocalDateTime dateSettled;

}
