package app.tradeflows.api.order_service.clients;

import app.tradeflows.api.order_service.config.ExchangeServerProperties;
import app.tradeflows.api.order_service.dtos.OrderDTO;
import app.tradeflows.api.order_service.dtos.exchange.CheckStatusDTO;
import app.tradeflows.api.order_service.dtos.exchange.ExecuteOrderDTO;
import app.tradeflows.api.order_service.dtos.exchange.OrderBookDTO;
import app.tradeflows.api.order_service.dtos.exchange.ProductDTO;
import app.tradeflows.api.order_service.enums.ExchangeServer;
import app.tradeflows.api.order_service.enums.OrderBookFilter;
import app.tradeflows.api.order_service.enums.OrderStatus;
import app.tradeflows.api.order_service.enums.TradeStatus;
import app.tradeflows.api.order_service.exceptions.InvalidOrderException;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class ExchangeServerClient {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeServerClient.class);
    private final RestTemplate restTemplate;
    @Setter
    private ExchangeServer server;
    private final ExchangeServerProperties serverProperties;

    public ExchangeServerClient(ExchangeServerProperties serverProperties){
        this.restTemplate = new RestTemplate();
        this.serverProperties = serverProperties;
    }

    private String getExchangeServerBaseUrl(){
        if(Objects.equals(server.getType(), "MAL1")){
            return serverProperties.getBaseUrl1();
        }

        return serverProperties.getBaseUrl2();
    }

    private String getExchangeServerKey(){
        if(Objects.equals(server.getType(), "MAL1")){
            return serverProperties.getApiKey1();
        }

        return  serverProperties.getApiKey2();
    }

    // Get Products
    public List<ProductDTO> getProducts(){
        var url = getExchangeServerBaseUrl()+"/pd";
        logger.info("Getting all products from {}", url);
        var response = this.restTemplate.exchange(url,
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ProductDTO>>() {});
        return response.getBody();
    }

    // Get Product by Ticker
    public ProductDTO getProductTicker(String ticker){
        var url = getExchangeServerBaseUrl()+"/pd/"+ticker;
        logger.info("Getting product by ticker from {}", url);
        var response = this.restTemplate.exchange(getExchangeServerBaseUrl()+"/pd/"+ticker,
                HttpMethod.GET, null,
                new ParameterizedTypeReference<ProductDTO>() {});
        return response.getBody();
    }

    // Check subscription
    public List<String> checkWebhookSubscription(){
        var url = getExchangeServerBaseUrl()+"/pd/subscription";
        logger.info("Getting list of subscription {}", url);
        var response = this.restTemplate.exchange(url,
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<String>>() {});
        return response.getBody();
    }

    // Subscribe to product notification
    public Boolean subscribeWebhook(String webhookUrl){
        var url = getExchangeServerBaseUrl()+"/pd/subscription";
        logger.info("Subscribing to webhook {}", url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> request = new HttpEntity<>(webhookUrl, headers);
        var response = this.restTemplate.exchange(url,
                HttpMethod.POST, request,
                new ParameterizedTypeReference<Boolean>() {});
        return response.getBody();
    }

    // Get orderBooks
    public List<OrderBookDTO> getAllOrderBooks(){
        var url = getExchangeServerBaseUrl()+"/orderbook";
        var response = this.restTemplate.exchange(url,
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<OrderBookDTO>>() {});
        return response.getBody();
    }

    // Get orderBook by product and filter (buy/sell/open/closed/cancelled)
    public List<OrderBookDTO> getOrderBooksByProduct(String ticker, OrderBookFilter filter){
        var url = getExchangeServerBaseUrl()+"/orderbook/"+ticker+"/"+filter.getFilter();
        var response = this.restTemplate.exchange(url,
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<OrderBookDTO>>() {});
        return response.getBody();
    }

    // Create Order
    public String executeOrder(ExecuteOrderDTO orderDTO){
        var url = getExchangeServerBaseUrl()+"/"+getExchangeServerKey()+"/order";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ExecuteOrderDTO> orderRequest = new HttpEntity<>(orderDTO, headers);
        var response = this.restTemplate.exchange(url,
                HttpMethod.POST, orderRequest,
                String.class);
        return Objects.requireNonNull(response.getBody()).replaceAll("\"", "");
    }

    //Check Order Status
    public CheckStatusDTO confirmOrderStatus(String orderId) throws InvalidOrderException {
        var url = getExchangeServerBaseUrl()+"/"+getExchangeServerKey()+"/order/"+orderId;

        var response = this.restTemplate.exchange(url,
                HttpMethod.GET, null,
                new ParameterizedTypeReference<OrderBookDTO>() {});
        if(!response.getStatusCode().is2xxSuccessful()){
            throw new InvalidOrderException("Order with id: "+ orderId +" does not exist");
        }
        OrderBookDTO orderBookDTO = response.getBody();
        if(Objects.isNull(orderBookDTO)){
            throw new InvalidOrderException("Order with id: "+ orderId +" does not exist");
        }
        CheckStatusDTO statusDTO = new CheckStatusDTO();
        if(orderBookDTO.executions().isEmpty()){
            statusDTO.setTradeStatus(TradeStatus.PENDING);
            statusDTO.setPrice(orderBookDTO.cumulativePrice());
            return  statusDTO;
        }
        if(orderBookDTO.cumulativeQuantity() < orderBookDTO.quantity()){
            statusDTO.setTradeStatus(TradeStatus.PARTIALLY_FILLED);
            statusDTO.setPrice(orderBookDTO.cumulativePrice());
            statusDTO.setQuantity(orderBookDTO.cumulativeQuantity());
            return  statusDTO;
        }
        statusDTO.setTradeStatus(TradeStatus.FILLED);
        statusDTO.setPrice(orderBookDTO.cumulativePrice());
        statusDTO.setQuantity(orderBookDTO.cumulativeQuantity());
        return  statusDTO;
    }

    //Cancel Order
    public Boolean cancelOrder(String orderId){
        var url = getExchangeServerBaseUrl()+"/"+getExchangeServerKey()+"/order/"+orderId;

        var response = this.restTemplate.exchange(url,
                HttpMethod.DELETE, null,
                new ParameterizedTypeReference<Boolean>() {});
        return response.getBody();
    }

    //Update order price and quantity
    public Boolean updateOrder(String orderId, OrderDTO orderDTO){
        var url = getExchangeServerBaseUrl()+"/"+getExchangeServerKey()+"/order/"+orderId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<OrderDTO> orderRequest = new HttpEntity<>(orderDTO, headers);

        var response = this.restTemplate.exchange(url,
                HttpMethod.PUT, orderRequest,
                new ParameterizedTypeReference<Boolean>() {});
        return response.getBody();
    }
}
