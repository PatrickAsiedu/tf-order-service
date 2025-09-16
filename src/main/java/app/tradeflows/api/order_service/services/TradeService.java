package app.tradeflows.api.order_service.services;

import app.tradeflows.api.order_service.dtos.OrderDTOGet;
import app.tradeflows.api.order_service.entities.Trade;
import app.tradeflows.api.order_service.exceptions.NotFoundException;
import app.tradeflows.api.order_service.repositories.TradeRepository;

import java.util.List;

public class TradeService {

    private final TradeRepository tradeRepository;

    public TradeService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public void createTrade() {
        // send the order to a specific exchange, take the returned string id and save it in the
        // "exchangeServerReference" column/field
        // order_id field will be the id of the locally saved order which the trade was made for
        // settledUnit: this is updated with the cumulative qty value from the exchange
        // settledPrice: this is the price our system chose to place the order(trade) for on either exchange
        // an actual qty field will be needed, which the settledUnit field will be checked against after each
        // status check to determine if the trade status should also be updated or not.

    }

    public void checkTradeStatus() {

    }

    public Trade updateTrade(String id, OrderDTOGet orderDTO) {
        Trade tradeResult = tradeRepository.findById(id)
                .orElseThrow( () -> new NotFoundException("Trade was b=noty found") );

        // if the order cumulative qty is not equal to the trade settled unit, update the trade's settled units
        if (orderDTO.getCumulativeQuantity() != tradeResult.getSettledUnit())
            tradeResult.setSettledUnit(orderDTO.getCumulativeQuantity());

        return tradeResult;
    }

    public List<Trade> getAllTrades() {
        return tradeRepository.findAll();
    }

    public List<Trade> getTradesByOrderId(String orderId) {
        return tradeRepository.findByOrder_Id(orderId);
    }

    public Trade getTradeById(String id) {
        return tradeRepository.findById(id).orElseThrow( () -> new NotFoundException("Trade was b=noty found"));
    }

}
