package br.com.certamecards.errorreport.domain;

import java.util.Arrays;
import java.util.Optional;

public enum ErrorReportStatus {
    OPEN("open"),
    RESOLVED("resolved"),
    REJECTED("rejected");

    private final String code;

    ErrorReportStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<ErrorReportStatus> find(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst();
    }
}
