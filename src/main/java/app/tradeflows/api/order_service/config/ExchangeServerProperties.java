package app.tradeflows.api.order_service.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "exchange.server")
public class ExchangeServerProperties {
    private String baseUrl1;
    private String apiKey1;
    private String baseUrl2;
    private String apiKey2;

}
