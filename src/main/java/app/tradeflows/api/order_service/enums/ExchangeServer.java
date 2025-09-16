package app.tradeflows.api.order_service.enums;

public enum ExchangeServer {
    MAL1("MAL1"),
    MAL2("MAL2");

    private final String type;

    ExchangeServer(String type){
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
