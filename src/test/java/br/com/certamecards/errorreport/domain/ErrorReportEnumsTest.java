package br.com.certamecards.errorreport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ErrorReportEnumsTest {

    @Test
    void givenReasonCode_whenParsing_thenReturnsReason() {
        assertThat(ErrorReportReason.fromCode("wrong_answer")).isEqualTo(ErrorReportReason.WRONG_ANSWER);
        assertThat(ErrorReportReason.OUTDATED_CONTENT.code()).isEqualTo("outdated_content");
    }

    @Test
    void givenUnknownReasonCode_whenParsing_thenThrows() {
        assertThatThrownBy(() -> ErrorReportReason.fromCode("spam")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenStatusCode_whenFinding_thenReturnsStatusOrEmpty() {
        assertThat(ErrorReportStatus.find("rejected")).contains(ErrorReportStatus.REJECTED);
        assertThat(ErrorReportStatus.find("closed")).isEmpty();
    }
}
