package com.az.chatroom.config;

import com.az.chatroom.rest.AuthController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI chatroomOpenApi() {
        String bearerAuthKey = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Chatroom API")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(bearerAuthKey, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(bearerAuthKey));
    }

    @Bean
    public OperationCustomizer publicLoginOperation() {
        return (operation, handlerMethod) -> {
            if (isLoginEndpoint(handlerMethod)) {
                operation.setSecurity(List.of());
            }
            return operation;
        };
    }

    private boolean isLoginEndpoint(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().equals(AuthController.class)
                && handlerMethod.getMethod().getName().equals("login");
    }
}
