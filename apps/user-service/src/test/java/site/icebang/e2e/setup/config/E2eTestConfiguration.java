package site.icebang.e2e.setup.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class E2eTestConfiguration {

  @Bean
  public Network testNetwork() {
    return Network.newNetwork();
  }

  @Bean
  @ServiceConnection
  MariaDBContainer<?> mariadbContainer() {
    return new MariaDBContainer<>("mariadb:11.4")
        .withDatabaseName("pre_process")
        .withUsername("mariadb")
        .withPassword("qwer1234");
  }

  @Bean
  GenericContainer<?> lokiContainer(Network network) {
    return new GenericContainer<>(DockerImageName.parse("grafana/loki:2.9.0"))
        .withNetwork(network)
        .withNetworkAliases("loki")
        .withExposedPorts(3100)
        .withCommand("-config.file=/etc/loki/local-config.yaml")
        .waitingFor(Wait.forHttp("/ready"))
        .withStartupTimeout(java.time.Duration.ofMinutes(2));
  }

  @DynamicPropertySource
  static void configureProperties(
      DynamicPropertyRegistry registry, MariaDBContainer<?> mariadb, GenericContainer<?> loki) {
    // MariaDB 연결 설정
    registry.add("spring.datasource.url", () -> mariadb.getJdbcUrl() + "?serverTimezone=UTC");
    registry.add("spring.datasource.username", mariadb::getUsername);
    registry.add("spring.datasource.password", mariadb::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");

    // HikariCP 설정
    registry.add("spring.hikari.connection-timeout", () -> "30000");
    registry.add("spring.hikari.idle-timeout", () -> "600000");
    registry.add("spring.hikari.max-lifetime", () -> "1800000");
    registry.add("spring.hikari.maximum-pool-size", () -> "10");
    registry.add("spring.hikari.minimum-idle", () -> "5");
    registry.add("spring.hikari.pool-name", () -> "HikariCP-E2E");

    System.setProperty("loki.port", String.valueOf(loki.getMappedPort(3100)));
  }
}
