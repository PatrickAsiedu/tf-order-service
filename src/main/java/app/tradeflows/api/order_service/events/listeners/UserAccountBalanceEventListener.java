package app.tradeflows.api.order_service.events.listeners;

import app.tradeflows.api.order_service.config.JsonBuilder;
import app.tradeflows.api.order_service.config.KafkaProperties;
import app.tradeflows.api.order_service.dtos.AuditLogDto;
import app.tradeflows.api.order_service.events.AuditLogEvent;
import app.tradeflows.api.order_service.events.UserAccountBalanceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class UserAccountBalanceEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserAccountBalanceEventListener.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties properties;

    public UserAccountBalanceEventListener(KafkaTemplate<String, String> kafkaTemplate, KafkaProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @EventListener
    public void handleAuditLogEvent(UserAccountBalanceEvent event) {

        try {
            String payload = new JsonBuilder().gson().toJson(event.getUpdateDTO());
            kafkaTemplate.send(properties.getUpdateUserBalanceTopic(), payload);
        }catch (Exception ex){
            logger.error("An error occurred publishing to topic, {}", ex.getMessage());
        }
    }
}
