package br.com.certamecards.errorreport.domain;

import java.util.Arrays;

public enum ErrorReportReason {
    OUTDATED_CONTENT("outdated_content"),
    WRONG_ANSWER("wrong_answer"),
    TYPO("typo"),
    OTHER("other");

    private final String code;

    ErrorReportReason(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ErrorReportReason fromCode(String code) {
        return Arrays.stream(values())
                .filter(reason -> reason.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ErrorReportReason code: " + code));
    }
}
