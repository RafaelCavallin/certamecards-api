package br.com.certamecards.testsupport.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SeedRequest(@NotBlank String scenario, @Min(1) @Max(5000) Integer count, String subjectName) {

    private static final int DEFAULT_COUNT = 10;

    public int countOrDefault() {
        return count == null ? DEFAULT_COUNT : count;
    }
}
