package app.tradeflows.api.order_service.services;

import app.tradeflows.api.order_service.dtos.OrderDTO;
import app.tradeflows.api.order_service.entities.PortfolioProduct;
import app.tradeflows.api.order_service.entities.Product;
import app.tradeflows.api.order_service.enums.Side;
import app.tradeflows.api.order_service.exceptions.InvalidOrderException;
import app.tradeflows.api.order_service.repositories.PortfolioProductRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class Validator {
    @Autowired
    PortfolioProductRepository portfolioProductRepository;

    //todo: could make these methods static
    private boolean clientHasEnough(OrderDTO orderDTO) throws InvalidOrderException {
        if (orderDTO.getSide().equals(Side.SELL)){
            Optional<PortfolioProduct> portfolioProduct = Optional.ofNullable(portfolioProductRepository
                    .findByPortfolioIdAndProductId(
                            orderDTO.getPortfolioId(),
                            orderDTO.getProduct()
                    ));
            Product product;
            if (portfolioProduct.isPresent()) {
                product = portfolioProduct.get().getProduct();
                return ( orderDTO.getQuantity() <= portfolioProduct.get().getQuantity() )
                        && ( orderDTO.getQuantity() <= product.getSellLimit() );
            }

            return false;
//                throw new InvalidOrderException("The selected product does not exist in user's portfolio");

            //todo: check if user has that specific stock
            // if they own some, check if the order quantity => qty owned
        }

        return false;
    }

    public boolean balanceIsEnough(OrderDTO orderDTO) {
        double balance= 1; // get from user service cache (user's account)
        if (balance==0)
            return false;
        if (orderDTO.getSide().equals(Side.BUY))
            return balance > ( orderDTO.getQuantity() * orderDTO.getPrice() );

        return false;
    }


    public boolean priceIsValid(OrderDTO orderDTO) {
        double lastTradedPrice = 0;  // this will come from market data service
        double priceDiff = Math.abs(orderDTO.getPrice() - lastTradedPrice);
        double maxPriceShift = 0;

        String message = "Your current order is unlikely to succeed due to a price difference larger than the Max price shift.";

        return priceDiff <= maxPriceShift;
    }

    static boolean quantityIsValid(OrderDTO orderDTO) {
        return !(orderDTO.getQuantity() > 10000);
    }
}
