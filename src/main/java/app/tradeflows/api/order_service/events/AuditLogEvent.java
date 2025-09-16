package app.tradeflows.api.order_service.events;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AuditLogEvent extends ApplicationEvent {
    private final String userId;
    private final String action;
    private final String description;
    private final String role;
    private final HttpServletRequest request;

    public AuditLogEvent(Object source, String userId, String action, String description, String role, HttpServletRequest request) {
        super(source);
        this.userId = userId;
        this.action = action;
        this.description = description;
        this.role = role;
        this.request = request;
    }


}