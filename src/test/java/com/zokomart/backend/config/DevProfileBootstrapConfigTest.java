package com.zokomart.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DevProfileBootstrapConfigTest {

    @Test
    void devProfileEnablesBuyerBootstrapForLocalManualAuthenticationChecks() {
        ClassPathResource baseResource = new ClassPathResource("application.yml");
        ClassPathResource devResource = new ClassPathResource("application-dev.yml");

        assertThat(baseResource.exists()).isTrue();
        assertThat(devResource.exists()).isTrue();

        YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
        factoryBean.setResources(baseResource, devResource);

        Properties properties = factoryBean.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("server.port")).isEqualTo("8000");
        assertThat(properties.getProperty("zokomart.buyer.bootstrap.enabled")).isEqualTo("true");
        assertThat(properties.getProperty("zokomart.buyer.bootstrap.phone-number"))
                .isEqualTo("+233 24 567 8901");
        assertThat(properties.getProperty("zokomart.buyer.bootstrap.password")).isEqualTo("Passw0rd!");
        assertThat(properties.getProperty("zokomart.admin.bootstrap.enabled")).isEqualTo("true");
    }
}
