package br.com.certamecards.events.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductEventNameTest {

    @Test
    void givenEachCode_whenFromCode_thenReturnsMatchingConstant() {
        for (ProductEventName name : ProductEventName.values()) {
            assertThat(ProductEventName.fromCode(name.code())).isEqualTo(name);
        }
    }

    @Test
    void givenUnknownCode_whenFromCode_thenThrows() {
        assertThatThrownBy(() -> ProductEventName.fromCode("unknown")).isInstanceOf(IllegalArgumentException.class);
    }
}
