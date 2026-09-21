package br.com.certamecards.auditlog.persistence;

import br.com.certamecards.auditlog.domain.AdminAuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {}
