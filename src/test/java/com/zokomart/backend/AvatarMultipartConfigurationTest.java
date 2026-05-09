package com.zokomart.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AvatarMultipartConfigurationTest {

    @Autowired
    private MultipartProperties multipartProperties;

    @Test
    void multipartLimitsAllowMobileAvatarUploadsAboveDefaultOneMegabyte() {
        assertThat(multipartProperties.getMaxFileSize()).isGreaterThanOrEqualTo(DataSize.ofMegabytes(5));
        assertThat(multipartProperties.getMaxRequestSize()).isGreaterThanOrEqualTo(DataSize.ofMegabytes(6));
    }
}
