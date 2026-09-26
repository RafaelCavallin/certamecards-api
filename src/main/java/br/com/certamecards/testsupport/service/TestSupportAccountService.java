package br.com.certamecards.testsupport.service;

import br.com.certamecards.auth.service.ConfirmedAccountCreator;
import br.com.certamecards.auth.service.CreateConfirmedAccountCommand;
import br.com.certamecards.user.service.TermsProperties;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TestSupportAccountService {

    private final ConfirmedAccountCreator accountCreator;
    private final TermsProperties termsProperties;
    private final Clock clock;

    public TestSupportAccountService(
            ConfirmedAccountCreator accountCreator, TermsProperties termsProperties, Clock clock) {
        this.accountCreator = accountCreator;
        this.termsProperties = termsProperties;
        this.clock = clock;
    }

    public UUID createConfirmed(TestAccount account) {
        CreateConfirmedAccountCommand command = new CreateConfirmedAccountCommand(
                account.email(),
                account.password(),
                account.displayName(),
                account.timeZone(),
                termsProperties.currentVersion(),
                clock.instant());
        return accountCreator.create(command).getId();
    }
}
