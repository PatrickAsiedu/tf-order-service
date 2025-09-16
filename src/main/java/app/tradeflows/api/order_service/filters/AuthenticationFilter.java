package app.tradeflows.api.order_service.filters;

import app.tradeflows.api.order_service.dtos.CustomUser;
import app.tradeflows.api.order_service.exceptions.ForbiddenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {



    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        logger.info("======================================================");
        logger.info("X-AUTHENTICATED"+request.getHeader("X-AUTHENTICATED"));
        final boolean isAuthenticated = Boolean.parseBoolean(request.getHeader("X-AUTHENTICATED"));
        logger.info("isAuthenticated - "+isAuthenticated);

        if (!isAuthenticated) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null) {
                CustomUser userDetails = new CustomUser(request.getHeader("X-USER-EMAIL"),
                        request.getHeader("X-USER-ROLE"), request.getHeader("X-USER-ID"));
                System.out.println(userDetails);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception exception) {
            logger.error(exception.toString(), exception);
        } finally {
            filterChain.doFilter(request, response);
        }
    }
}
