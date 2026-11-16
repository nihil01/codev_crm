package com.crm.config;

import com.crm.entity.User;
import com.crm.enums.Role;
import com.crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email:}")
    private String bootstrapEmail;
    @Value("${app.bootstrap-admin.password:}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;
        if (bootstrapEmail.isBlank() || bootstrapPassword.length() < 12) {
            throw new IllegalStateException(
                "Set CRM_BOOTSTRAP_ADMIN_EMAIL and a strong CRM_BOOTSTRAP_ADMIN_PASSWORD (12+ characters)"
            );
        }
        userRepository.save(User.builder()
            .email(bootstrapEmail.trim().toLowerCase(java.util.Locale.ROOT))
            .password(passwordEncoder.encode(bootstrapPassword))
            .fullName("Admin")
            .role(Role.ADMIN)
            .active(true)
            .build());
        log.info("Initial admin account created");
    }
}
