package com.zokomart.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Configuration
public class AdminPasswordConfig {

    @Bean
    public AdminPasswordEncoder adminPasswordEncoder() {
        return new Pbkdf2AdminPasswordEncoder();
    }

    public interface AdminPasswordEncoder {

        String encode(CharSequence rawPassword);

        boolean matches(CharSequence rawPassword, String encodedPassword);
    }

    static final class Pbkdf2AdminPasswordEncoder implements AdminPasswordEncoder {

        private static final String PREFIX = "pbkdf2-sha256";
        private static final int ITERATIONS = 310_000;
        private static final int SALT_LENGTH = 16;
        private static final int KEY_LENGTH = 256;

        private final SecureRandom secureRandom = new SecureRandom();

        @Override
        public String encode(CharSequence rawPassword) {
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);
            byte[] hash = deriveKey(rawPassword, salt, ITERATIONS);
            return PREFIX
                    + "$" + ITERATIONS
                    + "$" + Base64.getEncoder().encodeToString(salt)
                    + "$" + Base64.getEncoder().encodeToString(hash);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            if (encodedPassword == null || encodedPassword.isBlank()) {
                return false;
            }

            String[] parts = encodedPassword.split("\\$", -1);
            if (parts.length != 4 || !PREFIX.equals(parts[0])) {
                return false;
            }

            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
                byte[] actualHash = deriveKey(rawPassword, salt, iterations);
                return MessageDigest.isEqual(expectedHash, actualHash);
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }

        private byte[] deriveKey(CharSequence rawPassword, byte[] salt, int iterations) {
            char[] passwordChars = rawPassword == null
                    ? new char[0]
                    : rawPassword.toString().toCharArray();
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, iterations, KEY_LENGTH);
            try {
                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                return secretKeyFactory.generateSecret(spec).getEncoded();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
                throw new IllegalStateException("Failed to encode admin password", exception);
            } finally {
                spec.clearPassword();
            }
        }
    }
}
