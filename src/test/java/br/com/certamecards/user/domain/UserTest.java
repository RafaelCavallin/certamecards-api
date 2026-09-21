package br.com.certamecards.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");

    @Test
    void givenEmailDisplayNameAndRole_whenConstructing_thenGettersReturnValues() {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);
        assertThat(user.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(user.getDisplayName()).isEqualTo("Ana");
        assertThat(user.getRole()).isEqualTo(UserRole.CANDIDATE);
        assertThat(user.getId()).isNotNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getEmailVerifiedAt()).isNull();
        assertThat(user.getTerms().getAcceptedAt()).isNull();
        assertThat(user.getTerms().getVersion()).isNull();
    }

    @Test
    void givenUser_whenVerifyingEmail_thenEmailVerifiedAtIsSet() {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);
        user.verifyEmail(NOW);
        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    void givenUser_whenChangingPasswordHash_thenGetterReturnsNewHash() {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);
        user.changePasswordHash("hash");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void givenUser_whenChangingDisplayName_thenGetterReturnsNewName() {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);
        user.changeDisplayName("Ana Paula");
        assertThat(user.getDisplayName()).isEqualTo("Ana Paula");
    }

    @Test
    void givenUser_whenAcceptingTerms_thenTermsReflectVersionAndInstant() {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);
        user.acceptTerms("2026-09-01", NOW);
        assertThat(user.getTerms().getVersion()).isEqualTo("2026-09-01");
        assertThat(user.getTerms().getAcceptedAt()).isEqualTo(NOW);
    }

    @Test
    void givenCandidate_whenPromotingToAdmin_thenRoleIsAdmin() {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);
        user.promoteToAdmin();
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void givenTermsNulledByPersistenceProvider_whenPostLoadRuns_thenTermsIsReinitialized() throws Exception {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);
        setTermsToNull(user);
        invokePostLoad(user);
        assertThat(user.getTerms()).isNotNull();
        assertThat(user.getTerms().getAcceptedAt()).isNull();
    }

    private void setTermsToNull(User user) throws Exception {
        var field = User.class.getDeclaredField("terms");
        field.setAccessible(true);
        field.set(user, null);
    }

    private void invokePostLoad(User user) throws Exception {
        Method method = User.class.getDeclaredMethod("ensureTermsInitialized");
        method.setAccessible(true);
        method.invoke(user);
    }
}
