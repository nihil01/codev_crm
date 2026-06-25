package com.crm.config;

import com.crm.entity.User;
import com.crm.enums.Role;
import com.crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder()
                .email("admin@codev.az")
                .password(passwordEncoder.encode("C0dEv2@26!!"))
                .fullName("Admin")
                .role(Role.ADMIN)
                .active(true)
                .build());

            log.info("Default admin created: admin@codev.az");
        }
    }
}
