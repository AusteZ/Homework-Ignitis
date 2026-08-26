package com.az.chatroom.config;

import com.az.chatroom.enums.UserRole;
import com.az.chatroom.utils.JwtCustomClaim;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String API_DOCS_PATH = "/v3/api-docs/**";
    private static final String SWAGGER_UI_PATH = "/swagger-ui/**";
    private static final String SWAGGER_UI_HTML_PATH = "/swagger-ui.html";
    private static final String ADMIN_PATHS = "/api/admin/**";
    private static final String API_PATHS = "/api/**";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(
                                LOGIN_PATH,
                                API_DOCS_PATH,
                                SWAGGER_UI_PATH,
                                SWAGGER_UI_HTML_PATH
                        ).permitAll()
                        .requestMatchers(ADMIN_PATHS).hasAuthority(UserRole.ADMIN.authority())
                        .requestMatchers(API_PATHS).hasAnyAuthority(
                                UserRole.USER.authority(),
                                UserRole.ADMIN.authority()
                        )
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(SecurityProperties securityProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey(securityProperties.jwt().secret())));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecurityProperties securityProperties) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey(securityProperties.jwt().secret()))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of(
                new SimpleGrantedAuthority(UserRole.valueOf(jwt.getClaimAsString(JwtCustomClaim.ROLE)).authority())
        ));
        return converter;
    }

    private SecretKey jwtSecretKey(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
