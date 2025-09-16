package app.tradeflows.api.order_service.events.listeners;

import app.tradeflows.api.order_service.clients.ExchangeServerClient;
import app.tradeflows.api.order_service.config.JsonBuilder;
import app.tradeflows.api.order_service.dtos.WebhookDTO;
import app.tradeflows.api.order_service.entities.Order;
import app.tradeflows.api.order_service.entities.PortfolioProduct;
import app.tradeflows.api.order_service.entities.Trade;
import app.tradeflows.api.order_service.enums.OrderStatus;
import app.tradeflows.api.order_service.enums.Side;
import app.tradeflows.api.order_service.enums.TradeStatus;
import app.tradeflows.api.order_service.events.publishers.OrderStatusUpdateEventPublisher;
import app.tradeflows.api.order_service.exceptions.InvalidOrderException;
import app.tradeflows.api.order_service.repositories.OrderRepository;
import app.tradeflows.api.order_service.repositories.PortfolioProductRepository;
import app.tradeflows.api.order_service.repositories.TradeRepository;
import app.tradeflows.api.order_service.services.PortfolioProductService;
import app.tradeflows.api.order_service.services.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Component
public class KafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private final PortfolioService portfolioService;
    private final OrderStatusUpdateEventPublisher orderStatusUpdateEventPublisher;

    public KafkaConsumer(PortfolioService portfolioService, OrderStatusUpdateEventPublisher orderStatusUpdateEventPublisher) {
        this.portfolioService = portfolioService;
        this.orderStatusUpdateEventPublisher = orderStatusUpdateEventPublisher;
    }

    @KafkaListener(topics = "${spring.kafka.topic.create-user-portfolio-topic}")
    public void consumePortfolioCreationMessage(String userId) {
        logger.info("Received request to create default portfolio for user {} ", userId);
        portfolioService.createDefaultPortfolio(userId);
    }

    @KafkaListener(topics = "${spring.kafka.topic.market-data-update-topic}")
    public void consumeMarketDataMessage(String message) throws InvalidOrderException {
        logger.info("Received message: " + message);
        WebhookDTO webhookDTO = new JsonBuilder().gson().fromJson(message, WebhookDTO.class);
        orderStatusUpdateEventPublisher.publishEvent(webhookDTO.getExchange(), webhookDTO.getOrderID());
    }

}
