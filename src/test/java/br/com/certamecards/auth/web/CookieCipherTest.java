package br.com.certamecards.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CookieCipherTest {

    private static final String KEY = "QabpsXTm+MPu629aETSuCTTfJn+qhnPjeQp3KPGn8CQ=";

    private final CookieCipher cipher = new CookieCipher(new CookieEncryptionProperties(KEY));

    @Test
    void givenPlaintext_whenEncryptingThenDecrypting_thenReturnsOriginalBytes() {
        byte[] plaintext = "hello-world".getBytes(StandardCharsets.UTF_8);

        String encrypted = cipher.encrypt(plaintext);
        byte[] decrypted = cipher.decrypt(encrypted);

        assertThat(new String(decrypted, StandardCharsets.UTF_8)).isEqualTo("hello-world");
    }

    @Test
    void givenSamePlaintextEncryptedTwice_whenComparing_thenCiphertextsDiffer() {
        byte[] plaintext = "hello-world".getBytes(StandardCharsets.UTF_8);

        String first = cipher.encrypt(plaintext);
        String second = cipher.encrypt(plaintext);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void givenTamperedCiphertext_whenDecrypting_thenThrowsIllegalStateException() {
        String encrypted = cipher.encrypt("hello".getBytes(StandardCharsets.UTF_8));
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "abcd";

        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }
}
