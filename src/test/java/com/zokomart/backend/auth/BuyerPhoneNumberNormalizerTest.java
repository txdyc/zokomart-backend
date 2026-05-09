package com.zokomart.backend.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuyerPhoneNumberNormalizerTest {

    @Test
    void normalizesGhanaLocalAndInternationalPhoneFormats() {
        assertThat(BuyerPhoneNumberNormalizer.normalize("024 567 8901")).isEqualTo("+233245678901");
        assertThat(BuyerPhoneNumberNormalizer.normalize("+233 24 567 8901")).isEqualTo("+233245678901");
        assertThat(BuyerPhoneNumberNormalizer.normalize("233245678901")).isEqualTo("+233245678901");
    }

    @Test
    void preservesUnknownDigitSequencesWithoutLosingLeadingPlus() {
        assertThat(BuyerPhoneNumberNormalizer.normalize("+234 801 234 5678")).isEqualTo("+2348012345678");
        assertThat(BuyerPhoneNumberNormalizer.normalize("801-234-5678")).isEqualTo("8012345678");
    }
}
