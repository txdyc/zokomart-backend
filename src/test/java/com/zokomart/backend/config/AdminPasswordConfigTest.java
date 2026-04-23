package com.zokomart.backend.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPasswordConfigTest {

    private final AdminPasswordConfig.AdminPasswordEncoder encoder = new AdminPasswordConfig().adminPasswordEncoder();

    @Test
    void encoderRejectsNoopAndMalformedHashes() {
        assertThat(encoder.matches("Passw0rd!", "{noop}Passw0rd!")).isFalse();
        assertThat(encoder.matches("Passw0rd!", "pbkdf2-sha256$bad-iterations$salt$hash")).isFalse();
        assertThat(encoder.matches("Passw0rd!", "pbkdf2-sha256$310000$%%%$hash")).isFalse();
        assertThat(encoder.matches("Passw0rd!", "not-a-real-hash")).isFalse();
    }
}
