package app.tradeflows.api.order_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class OpenApiConfig {
    String schemeName = "bearerAuth";
    String bearerFormat = "JWT";
    String scheme = "bearer";

    @Autowired
    private Environment environment;

    @Value("${server.servlet.context-path}")
    private String servletContext;

    @Bean
    public OpenAPI caseOpenAPI() {
        String portString = environment.getProperty("server.port");
        int port = portString != null ? Integer.parseInt(portString) : 8080;
        Server localServer = new Server();
        localServer.setDescription("Local Environment");
        localServer.setUrl("http://localhost:"+port+servletContext);

        Server prodServer = new Server();
        prodServer.setDescription("Prod Environment");
        prodServer.setUrl("https://api.tradeflows.app"+servletContext);

        Contact contact = new Contact();
        contact.name("Trade flows Support");
        contact.email("support@tradeflows.app");
        contact.url("https://tradeflows.app/");

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement()
                        .addList(schemeName)).components(new Components()
                        .addSecuritySchemes(
                                schemeName, new SecurityScheme()
                                        .name(schemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .bearerFormat(bearerFormat)
                                        .in(SecurityScheme.In.HEADER)
                                        .scheme(scheme)
                        )
                )
                .info(new Info()
                        .title("Trade Flow Order Service")
                        .description("Trade Flow Order Service OpenApi documentation")
                        .version("1.0")
                        .termsOfService("https://tradeflows.app/terms")
                        .contact(contact)
                )
                .servers(List.of(localServer, prodServer));
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        source.registerCorsConfiguration(servletContext+"/v3/api-docs", config);
        return new CorsFilter(source);
    }
}
