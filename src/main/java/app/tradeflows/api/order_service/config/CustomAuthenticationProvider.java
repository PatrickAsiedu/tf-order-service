package app.tradeflows.api.order_service.config;

import app.tradeflows.api.order_service.dtos.CustomUser;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    private final UserDetailsService userDetailsService;
    public CustomAuthenticationProvider(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        // Perform your custom authentication logic here
        // Retrieve user details from userDetailsService and validate the credentials
        // You can throw AuthenticationException if authentication fails
        // Example: retrieving user details by username from UserDetailsService
        CustomUser userDetails = (CustomUser) userDetailsService.loadUserByUsername(username);
        if (userDetails == null) {
            throw new UsernameNotFoundException("User not found");
        }
        // Create a fully authenticated Authentication object
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }
    @Override
    public boolean supports(Class<?> authentication) {
        // Return true if this AuthenticationProvider supports the provided authentication class
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
