package app.tradeflows.api.order_service.services;

import app.tradeflows.api.order_service.config.JsonBuilder;
import app.tradeflows.api.order_service.config.KafkaProperties;
import app.tradeflows.api.order_service.dtos.AccountDTO;
import app.tradeflows.api.order_service.dtos.OrderDTO;
import app.tradeflows.api.order_service.dtos.OrderDTOGet;
import app.tradeflows.api.order_service.dtos.UserBalanceUpdateDTO;
import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.entities.Portfolio;
import app.tradeflows.api.order_service.entities.PortfolioProduct;
import app.tradeflows.api.order_service.entities.Product;
import app.tradeflows.api.order_service.enums.BalanceAction;
import app.tradeflows.api.order_service.enums.OrderStatus;
import app.tradeflows.api.order_service.enums.UpdateType;
import app.tradeflows.api.order_service.events.publishers.UserAccountBalanceEventPublisher;
import app.tradeflows.api.order_service.exceptions.InsufficientBalanceException;
import app.tradeflows.api.order_service.exceptions.InsufficientStocksException;
import app.tradeflows.api.order_service.exceptions.InvalidOrderException;
import app.tradeflows.api.order_service.exceptions.NotFoundException;
import app.tradeflows.api.order_service.repositories.OrderRepository;
import app.tradeflows.api.order_service.repositories.PortfolioProductRepository;
import app.tradeflows.api.order_service.repositories.PortfolioRepository;
import app.tradeflows.api.order_service.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    protected String key = "c385452d-a1bd-44e6-94c8-ca6c6286ce0d";
    protected String exchangeUrl = "https://exchange.matraining.com"+"/"+key + "/order";

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    private final PortfolioRepository portfolioRepository;
    private final ProductRepository productRepository;
    private final PortfolioProductRepository portfolioProductRepository;
    private final RedisService<Object> redisService;
    private final UserAccountBalanceEventPublisher userAccountBalanceEventPublisher;


    @Autowired
    public OrderService(OrderRepository orderRepository, RestTemplate restTemplate,
                        PortfolioRepository portfolioRepository, ProductRepository productRepository,
                        PortfolioProductRepository portfolioProductRepository,RedisService<Object> redisService,
                        UserAccountBalanceEventPublisher userAccountBalanceEventPublisher) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
        this.portfolioRepository = portfolioRepository;
        this.productRepository = productRepository;
        this.portfolioProductRepository = portfolioProductRepository;
        this.redisService = redisService;
        this.userAccountBalanceEventPublisher = userAccountBalanceEventPublisher;
    }

    public ResponseEntity<String> sendOrder(OrderDTO orderDTO) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<OrderDTO> orderRequest = new HttpEntity<>(orderDTO, headers);

        ResponseEntity<String> orderResponse = null;
        try {
            orderResponse = restTemplate.exchange(exchangeUrl,
                    HttpMethod.POST,
                    orderRequest,
                    String.class);
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }
        return orderResponse;
    }

    public Order createOrder(OrderDTO orderDTO) throws InvalidOrderException, InsufficientBalanceException, InsufficientStocksException {
        boolean isValid = false;

        switch (orderDTO.getSide()) {
            case SELL -> {
                isValid = priceIsValid(orderDTO) && quantityIsValid(orderDTO) && clientHasEnoughStock(orderDTO);
            }
            case BUY -> {
                isValid = priceIsValid(orderDTO) && quantityIsValid(orderDTO) && balanceIsEnough(orderDTO);
            }
        }

        Order order = new Order();
        Order saveResult = null;

        if (isValid) {
            Portfolio portfolio = portfolioRepository.findById(orderDTO.getPortfolioId())
                    .orElseThrow(() -> new NotFoundException("Portfolio was not found."));

            Product product = productRepository.findByTicker(orderDTO.getProduct())
                    .orElseThrow(() -> new NotFoundException(orderDTO.getProduct() + " does not exist"));

            switch (orderDTO.getSide()) {
                case SELL -> {

                    PortfolioProduct portfolioProduct = portfolioProductRepository
                            .findByPortfolioIdAndProductId(orderDTO.getPortfolioId(), product.getId());

                    if(portfolioProduct.getQuantity() < orderDTO.getQuantity()){
                        throw new InsufficientStocksException("You don't have enough stock to make this order");
                    }

                    portfolioProduct.setQuantity(portfolioProduct.getQuantity() - orderDTO.getQuantity());
                    portfolioProduct.setLockedQuantity(portfolioProduct.getLockedQuantity() + orderDTO.getQuantity());

                    portfolioProductRepository.save(portfolioProduct);
                }
                case BUY -> {
                    UserBalanceUpdateDTO balanceUpdateDTO = new UserBalanceUpdateDTO();
                    balanceUpdateDTO.setDescription("Bought " + orderDTO.getQuantity() + " of " + orderDTO.getProduct());
                    balanceUpdateDTO.setAmount(orderDTO.getPrice());
                    balanceUpdateDTO.setAction(BalanceAction.DEBIT);
                    balanceUpdateDTO.setType(UpdateType.AVAILABLE_BALANCE);
                    balanceUpdateDTO.setUserId(orderDTO.getUserId());
                    userAccountBalanceEventPublisher.publishEvent(balanceUpdateDTO);
                }
            }
            order.setPrice(orderDTO.getPrice());
            order.setQuantity(orderDTO.getQuantity());
            order.setSide(orderDTO.getSide());
            order.setType(orderDTO.getType());
            order.setUserId(orderDTO.getUserId());

            order.setPortfolio(portfolio);
            order.setProduct(product);
            order.setCreatedAt(LocalDateTime.now());

            saveResult = orderRepository.save(order);
        }

        return saveResult;
    }

    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByUserIdAndStatus(String userId, String status) {
        if (status != null) {
            return orderRepository.findByUserIdAndStatus(userId, OrderStatus.valueOf(status.toUpperCase()));
        }
        else
            return orderRepository.findByUserId(userId);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findOrdersByStatus(status);
    }

    public List<Order> getPendingOrders(OrderStatus status) {
            return orderRepository.findOrdersByStatus(status);
    }

    public Order getOrderById(String id) throws NotFoundException {
        Optional<Order> order = orderRepository.findById(id);
        return order.orElseThrow(() -> new NotFoundException("Order does not exist"));
    }

    public OrderDTOGet getOrderFromExchangeById(String orderRef) {
        String url = exchangeUrl + "/" + orderRef;
        return restTemplate.getForEntity(url, OrderDTOGet.class).getBody();
    }

    public boolean cancelOrder(String id) throws NotFoundException {
        Order existingOrder = orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order does not exist"));

        existingOrder.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(existingOrder);

        return updated.getStatus().equals(OrderStatus.CANCELLED);
    }


    // helpers
    public boolean clientHasEnoughStock(OrderDTO orderDTO) throws InvalidOrderException {
        //todo: check if user has that specific stock (from the cache)

        Optional<PortfolioProduct> portfolioProduct = Optional.ofNullable(portfolioProductRepository
                .findByPortfolioIdAndProductId(
                        orderDTO.getPortfolioId(),
                        productRepository.findByTicker(orderDTO.getProduct())
                                .orElseThrow(() -> new NotFoundException("Product not found")).getId()
                ));

        if (portfolioProduct.isPresent()) {
            return ( orderDTO.getQuantity() <= portfolioProduct.get().getQuantity() );
        }
        else
            throw new InvalidOrderException("The selected product does not exist in the user's portfolio");
    }

    private boolean balanceIsEnough(OrderDTO orderDTO) throws InsufficientBalanceException {
        String cachedAccount = (String)redisService.getItem(orderDTO.getUserId());
        AccountDTO accountDTO = new JsonBuilder().gson().fromJson(cachedAccount, AccountDTO.class);
        double balance= accountDTO.getAvailableBalance();
        double orderValue = orderDTO.getQuantity() * orderDTO.getPrice();

        if (!(balance >= orderValue))
            throw new InsufficientBalanceException("Account balance is not enough to make this buy order");

        return true;
    }

    private boolean priceIsValid(OrderDTO orderDTO) throws InvalidOrderException {
        // todo: get product details from cache (focus is price)
        Product product = productRepository.findByTicker(orderDTO.getProduct())
                .orElseThrow(() -> new NotFoundException("Product does not exist"));

        double priceDiff=0;
        switch (orderDTO.getSide()) {
            case BUY -> priceDiff = Math.abs(orderDTO.getPrice() - product.getBidPrice());
            case SELL -> priceDiff = Math.abs(orderDTO.getPrice() - product.getAskPrice());
        }

        if (!(priceDiff <= product.getMaxShiftPrice()))
            throw new InvalidOrderException("Price is too high");

        return priceDiff <= product.getMaxShiftPrice();
    }

    private boolean quantityIsValid(OrderDTO orderDTO) throws InvalidOrderException {
        // todo: get product details from cache (focus is quantity)
        Product product = productRepository.findByTicker(orderDTO.getProduct())
                .orElseThrow( () -> new NotFoundException(orderDTO.getProduct() + " was not found"));

        boolean isValid = false;
        int limit = 0;
        switch (orderDTO.getSide()) {
            case BUY -> {
                limit = product.getBuyLimit();
                isValid = orderDTO.getQuantity() < product.getBuyLimit();
            }
            case SELL -> {
                limit = product.getSellLimit();
                isValid = orderDTO.getQuantity() < product.getSellLimit();
            }
        }
        if (!isValid)
            throw new InvalidOrderException("Quantity must be less than " + limit);

        return true;
    }

}
