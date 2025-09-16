package app.tradeflows.api.order_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
//@ResponseStatus
public class ResponseExceptionHandler {
    ProblemDetail errorDetail = null;

    @ExceptionHandler(InvalidOrderException.class)
    public ProblemDetail invalidOrderHandler(InvalidOrderException exception) {
        errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), exception.getMessage());
        errorDetail.setProperty("description", "Invalid Order");
        return errorDetail;
    }

    @ExceptionHandler(DefaultPortfolioException.class)
    public ProblemDetail defaultPortfolioHandler(DefaultPortfolioException defaultPortfolioException,
                                                               WebRequest request) {
        errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), defaultPortfolioException.getMessage());
        errorDetail.setProperty("description", "Default Portfolio Exception");
        return errorDetail;
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail insufficientBalanceException(InsufficientBalanceException insufficientBalanceException,
                                                                     WebRequest request) {
        errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), insufficientBalanceException.getMessage());
        errorDetail.setProperty("description", "Default Portfolio Exception");
        return errorDetail;
    }

    @ExceptionHandler(InsufficientStocksException.class)
    public ProblemDetail insufficientStocksException(InsufficientStocksException insufficientStocksException,
                                                                    WebRequest request) {
        errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), insufficientStocksException.getMessage());
        errorDetail.setProperty("description", "Default Portfolio Exception");
        return errorDetail;
    }

    @ExceptionHandler(InvalidPortfolioException.class)
    public ProblemDetail invalidPortfolioException(InvalidPortfolioException exception,
                                                                  WebRequest request) {
        errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), exception.getMessage());
        errorDetail.setProperty("description", "Default Portfolio Exception");
        return errorDetail;
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail authorizationDeniedException(AuthorizationDeniedException exception,
                                                                  WebRequest request) {
        errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), exception.getMessage());
        errorDetail.setProperty("description", "Default Portfolio Exception");
        return errorDetail;
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail forbiddenException(AuthorizationDeniedException exception,
                                                      WebRequest request) {
        errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403), exception.getMessage());
        errorDetail.setProperty("description", "Not allowed to access resource");
        return errorDetail;
    }
}
