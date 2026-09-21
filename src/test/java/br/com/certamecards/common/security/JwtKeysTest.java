package br.com.certamecards.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import org.junit.jupiter.api.Test;

class JwtKeysTest {

    private static final String PRIVATE_KEY =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg1ariM0FhiAI/MaqECWrlYxlGQ+K/uX4r7ii1T7odQGChRANCAASNXLS0GR2ZLRtv+MpoS3McDE7EB0F8X8fr12gx80da596aD7GSkdxfyVPu/xnho+OucXs+QMGLA0HfpG0f2CT6";
    private static final String PUBLIC_KEY =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEjVy0tBkdmS0bb/jKaEtzHAxOxAdBfF/H69doMfNHWufemg+xkpHcX8lT7v8Z4aPjrnF7PkDBiwNB36RtH9gk+g==";

    @Test
    void givenPemPrivateKey_whenParsing_thenReturnsEcPrivateKey() {
        ECPrivateKey key = JwtKeys.parsePrivateKey(PRIVATE_KEY);
        assertThat(key).isNotNull();
    }

    @Test
    void givenPemPublicKey_whenParsing_thenReturnsEcPublicKey() {
        ECPublicKey key = JwtKeys.parsePublicKey(PUBLIC_KEY);
        assertThat(key).isNotNull();
    }

    @Test
    void givenPemWithHeaderAndWhitespace_whenParsingPrivateKey_thenStillParses() {
        String wrapped = "-----BEGIN PRIVATE KEY-----\n" + PRIVATE_KEY + "\n-----END PRIVATE KEY-----\n";
        assertThat(JwtKeys.parsePrivateKey(wrapped)).isNotNull();
    }

    @Test
    void givenInvalidPrivateKey_whenParsing_thenThrowsIllegalStateException() {
        String validBase64ButNotAKey = java.util.Base64.getEncoder().encodeToString("not-a-real-key".getBytes());
        assertThatThrownBy(() -> JwtKeys.parsePrivateKey(validBase64ButNotAKey))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenInvalidPublicKey_whenParsing_thenThrowsIllegalStateException() {
        String validBase64ButNotAKey = java.util.Base64.getEncoder().encodeToString("not-a-real-key".getBytes());
        assertThatThrownBy(() -> JwtKeys.parsePublicKey(validBase64ButNotAKey))
                .isInstanceOf(IllegalStateException.class);
    }
}
