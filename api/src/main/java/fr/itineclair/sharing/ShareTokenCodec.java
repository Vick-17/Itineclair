package fr.itineclair.sharing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class ShareTokenCodec {

    private static final int TOKEN_BYTES = 32;
    private static final Pattern PRESENTED_TOKEN =
            Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final SecureRandom secureRandom;

    public ShareTokenCodec() {
        this(new SecureRandom());
    }

    ShareTokenCodec(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(
                secureRandom,
                "secureRandom");
    }

    TokenMaterial generate() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        return new TokenMaterial(token, sha256(token));
    }

    Optional<String> hashPresented(String token) {
        if (token == null || !PRESENTED_TOKEN.matcher(token).matches()) {
            return Optional.empty();
        }

        return Optional.of(sha256(token));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.",
                    exception);
        }
    }

    record TokenMaterial(
            String token,
            String hash) {
    }
}
