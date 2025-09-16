package app.tradeflows.api.order_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.kafka.topic")
public class KafkaProperties {
    private String auditLogReportingTopic;
    private String updateUserBalanceTopic;
}