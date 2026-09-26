package br.com.certamecards.testsupport.web;

import br.com.certamecards.testsupport.service.TestAccount;
import br.com.certamecards.testsupport.service.TestSupportAccountService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "certame.test-support", name = "enabled", havingValue = "true")
public class TestSupportAccountController {

    private final TestSupportAccountService accountService;

    public TestSupportAccountController(TestSupportAccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping(TestSupportSecurityConfig.USERS_PATH)
    @ResponseStatus(HttpStatus.CREATED)
    public TestUserResponse create(@Valid @RequestBody CreateTestUserRequest request) {
        TestAccount account =
                new TestAccount(request.email(), request.password(), request.displayName(), request.timeZone());
        return new TestUserResponse(accountService.createConfirmed(account));
    }
}
