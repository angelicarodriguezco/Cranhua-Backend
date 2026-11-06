package com.tuanistack.cranhua.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sesion")
public record SesionProperties(String secreto, Duration duracion) {}
