package com.zokomart.backend.config;

import com.zokomart.backend.auth.BuyerPasswordConfig;
import com.zokomart.backend.auth.BuyerPasswordConfig.BuyerPasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class BuyerPasswordConfigTest {

    @Test
    void encodeUsesBcryptAndMatchesRawPassword() {
        BuyerPasswordConfig config = new BuyerPasswordConfig();
        BuyerPasswordEncoder passwordEncoder = config.buyerPasswordEncoder();

        String encoded = passwordEncoder.encode("Passw0rd!");

        assertThat(encoded).startsWith("$2");
        assertThat(encoded).isNotEqualTo("Passw0rd!");
        assertThat(passwordEncoder.matches("Passw0rd!", encoded)).isTrue();
        assertThat(new BCryptPasswordEncoder().matches("Passw0rd!", encoded)).isTrue();
    }
}
