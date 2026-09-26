package br.com.certamecards.user.service;

import br.com.certamecards.common.security.UserAuthCache;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermsService {

    private final AccountService accountService;
    private final UserAuthCache userAuthCache;
    private final Clock clock;

    public TermsService(AccountService accountService, UserAuthCache userAuthCache, Clock clock) {
        this.accountService = accountService;
        this.userAuthCache = userAuthCache;
        this.clock = clock;
    }

    @Transactional
    public void accept(UUID userId, String version) {
        accountService.getProfile(userId).acceptTerms(version, clock.instant());
        userAuthCache.evictAfterCommit(userId);
    }
}
