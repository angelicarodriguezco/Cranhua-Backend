package com.tuanistack.cranhua.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

  @Bean
  Cloudinary cloudinary(CloudinaryProperties propiedades) {
    return new Cloudinary(
        ObjectUtils.asMap(
            "cloud_name",
            propiedades.cloudName(),
            "api_key",
            propiedades.apiKey(),
            "api_secret",
            propiedades.apiSecret(),
            "secure",
            true));
  }
}
