package br.com.certamecards.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminServiceTest {

    private final UUID actorId = UUID.randomUUID();
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminAuditRecorder auditRecorder = mock(AdminAuditRecorder.class);
    private final AdminService adminService = new AdminService(userRepository, auditRecorder);

    @Test
    void givenExistingCandidateEmail_whenGranting_thenUserIsPromotedAndAuditRecorded() {
        User candidate = new User("carla@exemplo.com", "Carla", UserRole.CANDIDATE);
        when(userRepository.findByEmail("carla@exemplo.com")).thenReturn(Optional.of(candidate));

        User result = adminService.grant(actorId, "Carla@Exemplo.com");

        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).save(candidate);
        verify(auditRecorder).recordGranted(actorId, candidate);
    }

    @Test
    void givenUnknownEmail_whenGranting_thenThrowsNotFound() {
        when(userRepository.findByEmail("ausente@exemplo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.grant(actorId, "ausente@exemplo.com"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void givenSingleAdmin_whenRevokingSelf_thenThrowsLastAdmin() {
        User onlyAdmin = new User("ana@exemplo.com", "Ana", UserRole.ADMIN);
        when(userRepository.findById(onlyAdmin.getId())).thenReturn(Optional.of(onlyAdmin));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminService.revoke(actorId, onlyAdmin.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.LAST_ADMIN));
        verify(userRepository, never()).save(onlyAdmin);
        verify(auditRecorder, never()).recordRevoked(actorId, onlyAdmin);
    }

    @Test
    void givenMultipleAdmins_whenRevokingOne_thenRoleBecomesCandidateAndAuditRecorded() {
        User admin = new User("ana@exemplo.com", "Ana", UserRole.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);

        adminService.revoke(actorId, admin.getId());

        assertThat(admin.getRole()).isEqualTo(UserRole.CANDIDATE);
        verify(userRepository).save(admin);
        verify(auditRecorder).recordRevoked(actorId, admin);
    }

    @Test
    void givenUnknownUserId_whenRevoking_thenThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.revoke(actorId, id))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void givenAdmins_whenListing_thenReturnsUsersWithAdminRole() {
        User admin = new User("ana@exemplo.com", "Ana", UserRole.ADMIN);
        when(userRepository.findAllByRole(UserRole.ADMIN)).thenReturn(List.of(admin));

        assertThat(adminService.listAdmins()).containsExactly(admin);
    }
}
