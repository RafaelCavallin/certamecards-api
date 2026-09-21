package br.com.certamecards.subject.web;

import br.com.certamecards.subject.domain.SubjectLimits;
import jakarta.validation.constraints.Size;

public record UpdateSubjectRequest(@Size(max = SubjectLimits.MAX_NAME_LENGTH) String name, Boolean active) {}
