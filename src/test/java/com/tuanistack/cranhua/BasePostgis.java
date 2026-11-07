package com.tuanistack.cranhua;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// Misma imagen que docker-compose para que las migraciones con PostGIS corran en los tests
@TestConfiguration(proxyBeanMethods = false)
public class BasePostgis {

  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgis() {
    return new PostgreSQLContainer<>(
        DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres"));
  }
}
