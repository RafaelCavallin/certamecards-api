package br.com.certamecards.admin.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.service.AdminAuditLogger;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminAuditRecorderTest {

    private final AdminAuditLogger auditLogger = mock(AdminAuditLogger.class);
    private final AdminAuditRecorder recorder = new AdminAuditRecorder(auditLogger);
    private final UUID actorId = UUID.randomUUID();

    @Test
    void givenPromotedUser_whenRecordingGranted_thenLogsAdminGrantedWithoutEmail() {
        User user = new User("carla@exemplo.com", "Carla", UserRole.ADMIN);

        recorder.recordGranted(actorId, user);

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.ADMIN_GRANTED),
                        eq(AuditTargetType.ADMIN_ROLE),
                        eq(user.getId()),
                        eq("Carla"),
                        any(Map.class));
    }

    @Test
    void givenDemotedUser_whenRecordingRevoked_thenLogsAdminRevokedWithoutEmail() {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);

        recorder.recordRevoked(actorId, user);

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.ADMIN_REVOKED),
                        eq(AuditTargetType.ADMIN_ROLE),
                        eq(user.getId()),
                        eq("Ana"),
                        any(Map.class));
    }
}
