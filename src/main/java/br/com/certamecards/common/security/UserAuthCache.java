package br.com.certamecards.common.security;

import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class UserAuthCache {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final UserRepository userRepository;
    private final Clock clock;
    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    public UserAuthCache(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public Optional<AuthSnapshot> snapshotOf(UUID userId) {
        Entry cached = cache.get(userId);
        if (cached != null && cached.expiresAt.isAfter(clock.instant())) {
            return cached.snapshot;
        }
        Optional<AuthSnapshot> snapshot = userRepository.findById(userId).map(UserAuthCache::toSnapshot);
        cache.put(userId, new Entry(snapshot, clock.instant().plus(TTL)));
        return snapshot;
    }

    private static AuthSnapshot toSnapshot(User user) {
        return new AuthSnapshot(user.getRole(), user.getTerms().getAcceptedAt() != null);
    }

    private record Entry(Optional<AuthSnapshot> snapshot, Instant expiresAt) {}
}
