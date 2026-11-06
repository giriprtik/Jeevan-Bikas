package com.hospital.auth.config;

import com.hospital.auth.dto.UserRegisterRequest;
import com.hospital.auth.repository.UserAccountRepository;
import com.hospital.auth.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates one account per role on first start so the system can be demoed out of the box.
 * Idempotent (skips usernames that already exist). Disable with APP_SEED_DEMO_USERS=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo-users", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AuthService authService;
    private final UserAccountRepository repository;
    private final String password;

    public DemoDataSeeder(AuthService authService, UserAccountRepository repository,
                          @Value("${app.seed.demo-password:Demo@1234}") String password) {
        this.authService = authService;
        this.repository = repository;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<UserRegisterRequest> users = List.of(
            user("admin",   "admin@jeevanbikas.local",   "ADMIN",   "System Administrator", null,          null,        null),
            user("nurse",   "nurse@jeevanbikas.local",   "NURSE",   "Sita Thapa",           "GENERAL_OPD", null,        null),
            user("doctor",  "doctor@jeevanbikas.local",  "DOCTOR",  "Dr. Ramesh Sharma",    "GENERAL_OPD", "NMC-10231", "General Medicine"),
            user("drkarki", "drkarki@jeevanbikas.local", "DOCTOR",  "Dr. Anjali Karki",     "CARDIOLOGY",  "NMC-11877", "Cardiology"),
            user("labtech", "labtech@jeevanbikas.local", "LABTECH", "Bikash Rai",           "LABORATORY",  null,        null));

        int created = 0;
        for (UserRegisterRequest u : users) {
            if (!repository.existsByUsername(u.getUsername())) {
                authService.registerNewUser(u);
                created++;
            }
        }
        if (created > 0) log.info("Seeded {} demo user(s); password for all: {}", created, password);
    }

    private UserRegisterRequest user(String username, String email, String role, String fullName,
                                     String department, String license, String specialization) {
        return new UserRegisterRequest(username, email, password, role, fullName, department, license, specialization);
    }
}
