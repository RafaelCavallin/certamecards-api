package br.com.certamecards.subject.web;

import br.com.certamecards.subject.domain.SubjectLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(@NotBlank @Size(max = SubjectLimits.MAX_NAME_LENGTH) String name) {}
