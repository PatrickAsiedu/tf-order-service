package app.tradeflows.api.order_service.controllers;

import app.tradeflows.api.order_service.dtos.OrderDTO;
import app.tradeflows.api.order_service.dtos.OrderDTOGet;
import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.enums.OrderStatus;
import app.tradeflows.api.order_service.exceptions.InsufficientBalanceException;
import app.tradeflows.api.order_service.exceptions.InsufficientStocksException;
import app.tradeflows.api.order_service.exceptions.InvalidOrderException;
import app.tradeflows.api.order_service.exceptions.NotFoundException;
import app.tradeflows.api.order_service.services.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/orders")
@RestController
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @PostMapping("/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Order> createOrder(
            @Valid @RequestBody OrderDTO orderDTO) throws InvalidOrderException, InsufficientBalanceException, InsufficientStocksException {
        Order order = orderService.createOrder(orderDTO);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Order>> getAllOrders() throws NotFoundException {
        List<Order> orderList = orderService.getOrders();
        return ResponseEntity.ok(orderList);
    }

    @GetMapping("/all/pending")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Order>> getPendingOrders(@RequestParam(value = "filter") String filter) {
        List<Order> pendingOrders = orderService.getPendingOrders(OrderStatus.valueOf(filter.toUpperCase()));
        return ResponseEntity.ok(pendingOrders);
    }

    @GetMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Order>> getAllOrdersByUserIdAndStatus(
            @PathVariable(value = "userId") String userId,
            @RequestParam(value ="status", required = false) String status) throws NotFoundException {
        List<Order> orderList = orderService.getOrdersByUserIdAndStatus(userId, status);
        return ResponseEntity.ok(orderList);
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Order>> getAllOrdersByStatus(
            @RequestParam(value = "filter", required = false) String filter) throws NotFoundException {
        List<Order> orderList = orderService.getOrdersByStatus(OrderStatus.valueOf(filter.toUpperCase()));
        return ResponseEntity.ok(orderList);
    }

    @GetMapping("/order/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Order> getOrderById(@PathVariable(value = "id") String id) throws NotFoundException {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok().body(order);
    }

    @GetMapping("/exc-order/{ordRef}")
    public ResponseEntity<OrderDTOGet> getOrderFromExchangeById(@PathVariable(value = "ordRef") String orderRef) throws NotFoundException {
        OrderDTOGet order = orderService.getOrderFromExchangeById(orderRef);
        return ResponseEntity.ok().body(order);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PutMapping("/{id}")
    public boolean cancelOrder(@PathVariable(value = "id") String id) throws NotFoundException {
        Order order = orderService.getOrderById(id);
        return orderService.cancelOrder(order.getId());
    }
}
