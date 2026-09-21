package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.LoginAttempt;
import br.com.certamecards.auth.persistence.LoginAttemptRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginThrottle {

    private final LoginAttemptRepository repository;
    private final LoginThrottleProperties properties;
    private final Clock clock;

    public LoginThrottle(LoginAttemptRepository repository, LoginThrottleProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public void ensureAllowed(String email) {
        List<LoginAttempt> recentFailures = repository.findByEmailAndSuccessFalseOrderByAttemptedAtDesc(
                email, PageRequest.of(0, properties.maxFailures()));
        if (recentFailures.size() < properties.maxFailures()) {
            return;
        }
        Instant oldestOfWindow = recentFailures.get(recentFailures.size() - 1).getAttemptedAt();
        Instant unlockAt = oldestOfWindow.plus(properties.lockWindow());
        if (!unlockAt.isAfter(clock.instant())) {
            return;
        }
        long retrySeconds = Duration.between(clock.instant(), unlockAt).getSeconds();
        throw ApiException.withRetryAfter(ErrorCode.LOGIN_LOCKED, (int) Math.max(retrySeconds, 1));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String email, String ip) {
        repository.save(new LoginAttempt(email, ip, false, clock.instant()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String email, String ip) {
        repository.save(new LoginAttempt(email, ip, true, clock.instant()));
    }
}
