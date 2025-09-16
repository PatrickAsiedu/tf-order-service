package app.tradeflows.api.order_service.jobs;

import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.enums.OrderStatus;
import app.tradeflows.api.order_service.repositories.OrderRepository;
import app.tradeflows.api.order_service.services.OrderExecutionEngine;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderProcessorJob {

    private final OrderRepository orderRepository;
    private final OrderExecutionEngine executionEngine;

    public OrderProcessorJob(OrderRepository orderRepository, OrderExecutionEngine executionEngine) {
        this.orderRepository = orderRepository;
        this.executionEngine = executionEngine;
    }

    @Scheduled(cron = "*/10 * * * * ?")
    public void run(){
        List<Order> orders = orderRepository.findDistinctByStatusOrStatusOrderByCreatedAtAsc(OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED);
        orders.forEach(executionEngine::process);
    }
}
