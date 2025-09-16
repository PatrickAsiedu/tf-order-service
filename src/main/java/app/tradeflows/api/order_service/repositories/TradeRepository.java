package app.tradeflows.api.order_service.repositories;

import app.tradeflows.api.order_service.entities.Trade;
import app.tradeflows.api.order_service.enums.OrderStatus;
import app.tradeflows.api.order_service.enums.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, String> {
    List<Trade> findByOrder_Id(String id);

    Optional<Trade> findByExchangeServerReference(String exchangeServerReference);

    List<Trade> findDistinctByTradeStatusOrTradeStatusOrderByCreatedAtAscUpdatedAtAsc(@NonNull TradeStatus tradeStatus, @NonNull TradeStatus tradeStatus1);

    List<Trade> findDistinctByTradeStatusOrTradeStatusOrOrder_StatusOrOrder_StatusOrderByCreatedAtAscUpdatedAtAsc(@NonNull TradeStatus tradeStatus, @NonNull TradeStatus tradeStatus1, @NonNull OrderStatus status, @NonNull OrderStatus status1);


}
