package br.com.certamecards.subject.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.service.AdminAuditLogger;
import br.com.certamecards.subject.domain.Subject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubjectAuditRecorderTest {

    private final AdminAuditLogger auditLogger = mock(AdminAuditLogger.class);
    private final SubjectAuditRecorder recorder = new SubjectAuditRecorder(auditLogger);
    private final UUID actorId = UUID.randomUUID();

    @Test
    void givenNewSubject_whenRecordingCreated_thenLogsSubjectCreatedAction() {
        Subject subject = new Subject("Direito Penal", "direito penal");

        recorder.recordCreated(actorId, subject);

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.SUBJECT_CREATED),
                        eq(AuditTargetType.SUBJECT),
                        eq(subject.getId()),
                        eq("Direito Penal"),
                        any(Map.class));
    }

    @Test
    void givenDeactivation_whenRecordingActiveChanged_thenLogsSubjectDeactivatedAction() {
        Subject subject = new Subject("Direito Penal", "direito penal");

        recorder.recordActiveChanged(actorId, subject, true, false);

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.SUBJECT_DEACTIVATED),
                        eq(AuditTargetType.SUBJECT),
                        eq(subject.getId()),
                        eq("Direito Penal"),
                        any(Map.class));
    }

    @Test
    void givenReactivation_whenRecordingActiveChanged_thenLogsSubjectReactivatedAction() {
        Subject subject = new Subject("Direito Penal", "direito penal");

        recorder.recordActiveChanged(actorId, subject, false, true);

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.SUBJECT_REACTIVATED),
                        eq(AuditTargetType.SUBJECT),
                        eq(subject.getId()),
                        eq("Direito Penal"),
                        any(Map.class));
    }
}
