package com.xagent.pilot.starter.content.infrastructure.x;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class XTokenCryptoService {

    private static final String ALGORITHM =
            "AES/GCM/NoPadding";

    private static final int IV_LENGTH =
            12;

    private static final int TAG_LENGTH_BITS =
            128;

    private static final String VERSION_PREFIX =
            "v1:";

    private final SecretKeySpec secretKey;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public XTokenCryptoService(
            @Value("${XAGENT_TOKEN_ENCRYPTION_KEY}")
            String encryptionKey
    ) {

        byte[] keyBytes =
                Base64.getDecoder()
                        .decode(encryptionKey);

        if (keyBytes.length != 32) {

            throw new IllegalStateException(
                    "XAGENT_TOKEN_ENCRYPTION_KEY "
                            + "must decode to exactly 32 bytes"
            );
        }

        this.secretKey =
                new SecretKeySpec(
                        keyBytes,
                        "AES"
                );
    }

    public String encrypt(
            String plainText
    ) {

        if (plainText == null
                || plainText.isBlank()) {

            return null;
        }

        try {

            byte[] iv =
                    new byte[IV_LENGTH];

            secureRandom.nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance(
                            ALGORITHM
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(
                            TAG_LENGTH_BITS,
                            iv
                    )
            );

            byte[] encrypted =
                    cipher.doFinal(
                            plainText.getBytes(
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    );

            byte[] combined =
                    ByteBuffer
                            .allocate(
                                    iv.length
                                            + encrypted.length
                            )
                            .put(iv)
                            .put(encrypted)
                            .array();

            return VERSION_PREFIX
                    + Base64
                    .getEncoder()
                    .encodeToString(
                            combined
                    );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to encrypt X token",
                    exception
            );
        }
    }

    public String decrypt(
            String encryptedValue
    ) {

        if (encryptedValue == null
                || encryptedValue.isBlank()) {

            return null;
        }

        if (!encryptedValue.startsWith(
                VERSION_PREFIX
        )) {

            throw new IllegalStateException(
                    "Unsupported encrypted token format"
            );
        }

        try {

            String encoded =
                    encryptedValue.substring(
                            VERSION_PREFIX.length()
                    );

            byte[] combined =
                    Base64
                            .getDecoder()
                            .decode(encoded);

            if (combined.length <= IV_LENGTH) {

                throw new IllegalStateException(
                        "Encrypted token is invalid"
                );
            }

            byte[] iv =
                    new byte[IV_LENGTH];

            byte[] encrypted =
                    new byte[
                            combined.length
                                    - IV_LENGTH
                            ];

            System.arraycopy(
                    combined,
                    0,
                    iv,
                    0,
                    IV_LENGTH
            );

            System.arraycopy(
                    combined,
                    IV_LENGTH,
                    encrypted,
                    0,
                    encrypted.length
            );

            Cipher cipher =
                    Cipher.getInstance(
                            ALGORITHM
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(
                            TAG_LENGTH_BITS,
                            iv
                    )
            );

            byte[] decrypted =
                    cipher.doFinal(
                            encrypted
                    );

            return new String(
                    decrypted,
                    java.nio.charset.StandardCharsets.UTF_8
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to decrypt X token",
                    exception
            );
        }
    }
}