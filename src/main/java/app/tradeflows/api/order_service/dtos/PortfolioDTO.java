package app.tradeflows.api.order_service.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class PortfolioDTO {
    @NotEmpty
    private String name;
    @NotNull
    private String userId;
    private Boolean isdefault;
}
