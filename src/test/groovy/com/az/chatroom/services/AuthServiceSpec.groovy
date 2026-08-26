package com.az.chatroom.services

import com.az.chatroom.config.SecurityProperties
import com.az.chatroom.dtos.LoginRequest
import com.az.chatroom.enums.UserRole
import com.az.chatroom.repositories.UserRepository
import com.az.chatroom.utils.JwtCustomClaim
import com.az.generated.jooq.tables.records.AppUserRecord
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import spock.lang.Specification

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

import static com.az.chatroom.testutils.TestData.TEST_PASSWORD
import static com.az.chatroom.testutils.TestData.TEST_PASSWORD_HASH
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID
import static com.az.chatroom.testutils.UserTestHelper.userRecord

class AuthServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    PasswordEncoder passwordEncoder = Mock()
    JwtEncoder jwtEncoder = Mock()
    Clock clock = Clock.fixed(Instant.parse("2026-08-26T01:00:00Z"), ZoneOffset.UTC)
    SecurityProperties securityProperties = new SecurityProperties(
            new SecurityProperties.Jwt("change-this-test-development-secret-key", Duration.ofHours(1)),
            new SecurityProperties.BootstrapAdmin("admin", "admin-password")
    )
    AuthService authService = new AuthService(userRepository, passwordEncoder, jwtEncoder, securityProperties, clock)

    def "should authenticate user and return jwt response"() {
        given:
        def user = userRecord()
        def jwt = Jwt.withTokenValue("token-value")
                .header("alg", "HS256")
                .subject(TEST_USERNAME)
                .issuedAt(clock.instant())
                .expiresAt(clock.instant().plusSeconds(3600))
                .claim(JwtCustomClaim.USER_ID, TEST_USER_ID.toString())
                .claim(JwtCustomClaim.ROLE, UserRole.USER.name())
                .build()

        when:
        def response = authService.login(new LoginRequest(TEST_USERNAME, TEST_PASSWORD))

        then:
        1 * userRepository.findByUsername(TEST_USERNAME) >> Optional.of(user)
        1 * passwordEncoder.matches(TEST_PASSWORD, TEST_PASSWORD_HASH) >> true
        1 * jwtEncoder.encode(_ as JwtEncoderParameters) >> jwt

        verifyAll(response) {
            token() == "token-value"
            userId() == TEST_USER_ID
            username() == TEST_USERNAME
            role() == UserRole.USER
            expiresAt() == clock.instant().plusSeconds(3600)
        }
        0 * _
    }

    def "should reject invalid password"() {
        given:
        AppUserRecord user = userRecord()

        when:
        authService.login(new LoginRequest(TEST_USERNAME, "wrong-password"))

        then:
        1 * userRepository.findByUsername(TEST_USERNAME) >> Optional.of(user)
        1 * passwordEncoder.matches("wrong-password", TEST_PASSWORD_HASH) >> false
        thrown(BadCredentialsException)
        0 * _
    }

    def "should reject missing user"() {
        when:
        authService.login(new LoginRequest(TEST_USERNAME, TEST_PASSWORD))

        then:
        1 * userRepository.findByUsername(TEST_USERNAME) >> Optional.empty()
        thrown(BadCredentialsException)
        0 * _
    }
}
