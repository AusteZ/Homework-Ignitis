package com.az.chatroom.services;

import com.az.chatroom.config.SecurityProperties;
import com.az.chatroom.dtos.LoginRequest;
import com.az.chatroom.dtos.LoginResponse;
import com.az.chatroom.enums.UserRole;
import com.az.chatroom.repositories.UserRepository;
import com.az.chatroom.utils.JwtCustomClaim;
import com.az.generated.jooq.tables.records.AppUserRecord;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration tokenExpiresIn;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            SecurityProperties securityProperties,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.tokenExpiresIn = securityProperties.jwt().expiresIn();
        this.clock = clock;
    }

    public LoginResponse login(LoginRequest request) {
        AppUserRecord user = userRepository.findByUsername(request.username())
                .filter(foundUser -> passwordEncoder.matches(request.password(), foundUser.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(tokenExpiresIn);
        UserRole role = UserRole.valueOf(user.getRole());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("chatroom")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim(JwtCustomClaim.USER_ID, user.getId().toString())
                .claim(JwtCustomClaim.ROLE, role.name())
                .build();

        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
        return new LoginResponse(token, expiresAt, user.getId(), user.getUsername(), role);
    }
}
