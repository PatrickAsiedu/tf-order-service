package app.tradeflows.api.order_service.enums;

public enum OrderBookFilter {
    SELL("sell"),
    BUY("buy"),
    OPEN("open"),
    CLOSED("closed"),
    CANCELLED("cancelled");

    private final String filter;

    OrderBookFilter(String filter){
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }
}
