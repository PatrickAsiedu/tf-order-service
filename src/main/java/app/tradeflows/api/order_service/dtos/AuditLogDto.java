package app.tradeflows.api.order_service.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AuditLogDto {
    private String userId;
    private String action;
    private String description;
    private String ipAddress;
    private String role;
    private String systemComponent;
    private Map<String, String> metaData;
    private LocalDateTime timestamp;
}
