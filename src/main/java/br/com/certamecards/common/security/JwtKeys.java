package br.com.certamecards.common.security;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class JwtKeys {

    private static final String EC_ALGORITHM = "EC";

    private JwtKeys() {}

    public static ECPrivateKey parsePrivateKey(String pem) {
        byte[] bytes = decode(pem);
        try {
            KeyFactory factory = KeyFactory.getInstance(EC_ALGORITHM);
            return (ECPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JWT private key", e);
        }
    }

    public static ECPublicKey parsePublicKey(String pem) {
        byte[] bytes = decode(pem);
        try {
            KeyFactory factory = KeyFactory.getInstance(EC_ALGORITHM);
            return (ECPublicKey) factory.generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JWT public key", e);
        }
    }

    private static byte[] decode(String pem) {
        String cleaned = pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }
}
