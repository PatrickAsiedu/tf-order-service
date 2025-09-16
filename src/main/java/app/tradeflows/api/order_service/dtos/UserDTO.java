package app.tradeflows.api.order_service.dtos;

import app.tradeflows.api.order_service.enums.UserRole;
import lombok.Data;

@Data
public class UserDTO {
    private String id;
    private String name;
    private String email;
    private String password;
    private UserRole role;
    private String dob;
    private boolean isActive;
}
