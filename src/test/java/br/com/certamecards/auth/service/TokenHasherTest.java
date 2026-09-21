package br.com.certamecards.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

    @Test
    void givenNoInput_whenGeneratingRawToken_thenReturnsUniqueUrlSafeValues() {
        String first = TokenHasher.generateRawToken();
        String second = TokenHasher.generateRawToken();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).doesNotContain("+", "/", "=");
    }

    @Test
    void givenSameRawToken_whenHashingTwice_thenProducesSameHash() {
        String raw = "fixed-raw-token";

        assertThat(TokenHasher.hash(raw)).isEqualTo(TokenHasher.hash(raw));
    }

    @Test
    void givenDifferentRawTokens_whenHashing_thenProducesDifferentHashes() {
        assertThat(TokenHasher.hash("token-a")).isNotEqualTo(TokenHasher.hash("token-b"));
    }
}
