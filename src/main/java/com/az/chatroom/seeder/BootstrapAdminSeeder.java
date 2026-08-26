package com.az.chatroom.seeder;

import com.az.chatroom.config.SecurityProperties;
import com.az.chatroom.enums.UserRole;
import com.az.chatroom.repositories.UserRepository;
import com.az.generated.jooq.tables.records.AppUserRecord;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@NullMarked
public class BootstrapAdminSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties.BootstrapAdmin bootstrapAdmin;

    public BootstrapAdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SecurityProperties securityProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdmin = securityProperties.bootstrapAdmin();
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(bootstrapAdmin.username())) {
            return;
        }

        AppUserRecord admin = new AppUserRecord();
        admin.setId(UUID.randomUUID());
        admin.setUsername(bootstrapAdmin.username());
        admin.setRole(UserRole.ADMIN.name());
        admin.setCreatedAt(OffsetDateTime.now());
        admin.setPasswordHash(passwordEncoder.encode(bootstrapAdmin.password()));
        userRepository.create(admin);
    }
}
