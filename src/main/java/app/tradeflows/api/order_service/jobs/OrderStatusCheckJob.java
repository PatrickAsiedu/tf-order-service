package app.tradeflows.api.order_service.jobs;

import app.tradeflows.api.order_service.entities.Trade;
import app.tradeflows.api.order_service.enums.ExchangeServer;
import app.tradeflows.api.order_service.enums.OrderStatus;
import app.tradeflows.api.order_service.enums.TradeStatus;
import app.tradeflows.api.order_service.events.publishers.OrderStatusUpdateEventPublisher;
import app.tradeflows.api.order_service.repositories.TradeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderStatusCheckJob {

    private final TradeRepository tradeRepository;
    private final OrderStatusUpdateEventPublisher orderStatusUpdateEventPublisher;

    public OrderStatusCheckJob(TradeRepository tradeRepository, OrderStatusUpdateEventPublisher orderStatusUpdateEventPublisher) {
        this.tradeRepository = tradeRepository;
        this.orderStatusUpdateEventPublisher = orderStatusUpdateEventPublisher;
    }

    @Scheduled(cron = "*/35 * * * * ?")
    public void run(){
        List<Trade> trades = tradeRepository.findDistinctByTradeStatusOrTradeStatusOrOrder_StatusOrOrder_StatusOrderByCreatedAtAscUpdatedAtAsc(TradeStatus.PENDING, TradeStatus.PARTIALLY_FILLED, OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED);
        trades.forEach(trade -> {
            orderStatusUpdateEventPublisher.publishEvent(ExchangeServer.valueOf(trade.getExchangeServerId()), trade.getExchangeServerReference());
        });
    }
}
