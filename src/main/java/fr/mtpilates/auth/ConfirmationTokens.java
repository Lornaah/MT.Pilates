package fr.mtpilates.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Jetons de confirmation : seul leur empreinte SHA-256 est stockée, pour qu'une fuite
 * de la base ne permette pas de valider des inscriptions.
 */
final class ConfirmationTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private ConfirmationTokens() {
    }

    static String newToken() {
        final byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(final String token) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
