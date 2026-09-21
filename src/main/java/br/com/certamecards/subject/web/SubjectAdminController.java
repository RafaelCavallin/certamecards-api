package br.com.certamecards.subject.web;

import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.subject.service.SubjectService;
import br.com.certamecards.subject.service.UpdateSubjectCommand;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubjectAdminController {

    private final SubjectService subjectService;

    public SubjectAdminController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping("/api/admin/subjects")
    public List<SubjectAdminResponse> list() {
        return subjectService.listWithDeckCount().stream()
                .map(SubjectAdminResponse::from)
                .toList();
    }

    @PostMapping("/api/admin/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectAdminResponse create(
            @AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody CreateSubjectRequest request) {
        return SubjectAdminResponse.from(subjectService.create(principal.id(), request.name()));
    }

    @PatchMapping("/api/admin/subjects/{id}")
    public SubjectAdminResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubjectRequest request) {
        UpdateSubjectCommand command = new UpdateSubjectCommand(request.name(), request.active());
        return SubjectAdminResponse.from(subjectService.update(principal.id(), id, command));
    }
}
