package br.com.certamecards.errorreport.domain;

public final class ErrorReportLimits {

    public static final int NOTE_MAX_LENGTH = 500;
    public static final int AUDIT_LABEL_MAX_LENGTH = 160;
    public static final String REASON_PATTERN = "^(outdated_content|wrong_answer|typo|other)$";
    public static final String OUTCOME_PATTERN = "^(resolved|rejected)$";

    private ErrorReportLimits() {}
}
