package br.com.certamecards.review.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReviewKindTest {

    @Test
    void givenEachCode_whenFromCode_thenReturnsMatchingConstant() {
        for (ReviewKind kind : ReviewKind.values()) {
            assertThat(ReviewKind.fromCode(kind.code())).isEqualTo(kind);
        }
    }

    @Test
    void givenUnknownCode_whenFromCode_thenThrows() {
        assertThatThrownBy(() -> ReviewKind.fromCode("unknown")).isInstanceOf(IllegalArgumentException.class);
    }
}
