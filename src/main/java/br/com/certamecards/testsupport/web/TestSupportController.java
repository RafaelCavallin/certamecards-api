package br.com.certamecards.testsupport.web;

import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.testsupport.domain.SeedScenario;
import br.com.certamecards.testsupport.service.SeedCommand;
import br.com.certamecards.testsupport.service.TestSupportOfficialSeeder;
import br.com.certamecards.testsupport.service.TestSupportSeedService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "certame.test-support", name = "enabled", havingValue = "true")
public class TestSupportController {

    private final TestSupportSeedService seedService;
    private final TestSupportOfficialSeeder officialSeeder;

    public TestSupportController(TestSupportSeedService seedService, TestSupportOfficialSeeder officialSeeder) {
        this.seedService = seedService;
        this.officialSeeder = officialSeeder;
    }

    @PostMapping("/api/test-support/seed")
    @ResponseStatus(HttpStatus.CREATED)
    public SeedResponse seed(
            @AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody SeedRequest request) {
        SeedScenario scenario = SeedScenario.fromCode(request.scenario());
        SeedCommand command =
                new SeedCommand(principal.id(), scenario, request.countOrDefault(), request.subjectName());
        return SeedResponse.from(scenario.official() ? officialSeeder.seed(command) : seedService.seed(command));
    }
}
