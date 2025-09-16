package app.tradeflows.api.order_service.dtos;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;

public class CustomUser extends User {
    private final String userId;

    public CustomUser(String username, String role, String userId) {
        super(username, "", true, true, true, true, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}