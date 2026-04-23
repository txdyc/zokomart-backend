package com.zokomart.backend;

import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.common.AdminUserStatus;
import com.zokomart.backend.admin.common.AdminUserType;
import com.zokomart.backend.admin.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserServiceLockoutGuardTest {

    private static final String PASSWORD_HASH_FOR_PASSW0RD = "pbkdf2-sha256$310000$QWRtaW4tU2VlZC0yMDI2IQ==$cEFKomdOKsSly0xDaWVmRtTCPIx7HaHKR+8AYWwrwNo=";

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @MockBean
    private AdminAccessPolicy adminAccessPolicy;

    @Test
    void cannotDisableLastActivePlatformAdmin() {
        seedAdminUser("admin-platform-disabled-001", "platform.ops.disabled", "Platform Ops Disabled", "PLATFORM_ADMIN", "DISABLED");
        given(adminAccessPolicy.requirePlatformAdmin()).willReturn(new AdminSessionActor(
                "admin-platform-operator-001",
                "platform.guard.operator",
                "Platform Guard Operator",
                AdminUserType.PLATFORM_ADMIN,
                AdminUserStatus.ACTIVE,
                List.of()
        ));

        assertThatThrownBy(() -> adminUserService.disableUser("00000000-0000-0000-0000-000000000001"))
                .hasMessage("必须至少保留一个启用的平台管理员");

        String statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM admin_users WHERE id = ?",
                String.class,
                "00000000-0000-0000-0000-000000000001"
        );
        assertThat(statusValue).isEqualTo("ACTIVE");
    }

    @Test
    void concurrentDisableAttemptsCannotLeaveZeroActivePlatformAdmins() throws Exception {
        executeInCommittedTransaction(() ->
                seedAdminUser("admin-platform-second-001", "platform.ops.second", "Platform Ops Second", "PLATFORM_ADMIN", "ACTIVE")
        );
        given(adminAccessPolicy.requirePlatformAdmin()).willReturn(new AdminSessionActor(
                "admin-platform-operator-001",
                "platform.guard.operator",
                "Platform Guard Operator",
                AdminUserType.PLATFORM_ADMIN,
                AdminUserStatus.ACTIVE,
                List.of()
        ));

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<String>> futures = new ArrayList<>();
            futures.add(executorService.submit(() -> disableAfterSignal("00000000-0000-0000-0000-000000000001", ready, start)));
            futures.add(executorService.submit(() -> disableAfterSignal("admin-platform-second-001", ready, start)));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }

            assertThat(results.stream().filter(result -> result.startsWith("SUCCESS:")).count()).isLessThan(2);
            assertThat(results).allMatch(result ->
                    result.startsWith("SUCCESS:") || result.equals("LAST_PLATFORM_ADMIN_DISABLE_FORBIDDEN")
            );
            Integer activePlatformAdminCount = queryInCommittedTransaction(() ->
                    jdbcTemplate.queryForObject(
                            """
                                    SELECT COUNT(*)
                                    FROM admin_users
                                    WHERE user_type = 'PLATFORM_ADMIN'
                                      AND status = 'ACTIVE'
                                    """,
                            Integer.class
                    )
            );
            assertThat(activePlatformAdminCount).isGreaterThan(0);
        } finally {
            executorService.shutdownNow();
            executeInCommittedTransaction(() -> {
                jdbcTemplate.update(
                        """
                                UPDATE admin_users
                                SET status = 'ACTIVE',
                                    updated_at = CURRENT_TIMESTAMP
                                WHERE id = '00000000-0000-0000-0000-000000000001'
                                """
                );
                jdbcTemplate.update(
                        """
                                DELETE FROM admin_action_logs
                                WHERE entity_type = 'ADMIN_USER'
                                  AND action_code = 'DISABLE_ADMIN_USER'
                                  AND entity_id IN ('00000000-0000-0000-0000-000000000001', 'admin-platform-second-001')
                                """
                );
                jdbcTemplate.update("DELETE FROM admin_users WHERE id = ?", "admin-platform-second-001");
            });
        }
    }

    private void seedAdminUser(String id, String username, String displayName, String userType, String status) {
        jdbcTemplate.update(
                """
                        INSERT INTO admin_users (
                            id,
                            username,
                            display_name,
                            password_hash,
                            user_type,
                            status,
                            last_login_at,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                id,
                username,
                displayName,
                PASSWORD_HASH_FOR_PASSW0RD,
                userType,
                status
        );
    }

    private String disableAfterSignal(String userId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent disable attempt");
        }
        try {
            adminUserService.disableUser(userId);
            return "SUCCESS:" + userId;
        } catch (com.zokomart.backend.common.exception.BusinessException exception) {
            return exception.getCode();
        }
    }

    private void executeInCommittedTransaction(Runnable runnable) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> runnable.run());
    }

    private <T> T queryInCommittedTransaction(java.util.function.Supplier<T> supplier) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> supplier.get());
    }
}
