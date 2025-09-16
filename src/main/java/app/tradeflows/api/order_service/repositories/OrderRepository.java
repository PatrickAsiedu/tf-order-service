package app.tradeflows.api.order_service.repositories;

import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(String userId);

    @Query("SELECT o from Order o where o.status = :status")
    List<Order> findOrdersByStatus(@Param("status")OrderStatus status);

    List<Order> findByUserIdAndStatus(String id, OrderStatus status);

    List<Order> findDistinctByStatusOrStatusOrderByCreatedAtAsc(@NonNull OrderStatus status, @NonNull OrderStatus status1);
}
