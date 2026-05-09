package com.zokomart.backend.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class BuyerPasswordConfig {

    @Bean
    public BuyerPasswordEncoder buyerPasswordEncoder() {
        return new BCryptBuyerPasswordEncoder();
    }

    public interface BuyerPasswordEncoder {
        String encode(CharSequence rawPassword);

        boolean matches(CharSequence rawPassword, String encodedPassword);
    }

    static final class BCryptBuyerPasswordEncoder implements BuyerPasswordEncoder {

        private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

        @Override
        public String encode(CharSequence rawPassword) {
            return delegate.encode(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return delegate.matches(rawPassword, encodedPassword);
        }
    }
}
