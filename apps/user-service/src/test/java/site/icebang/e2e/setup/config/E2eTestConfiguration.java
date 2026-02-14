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

  private static final Network network = Network.newNetwork();

  private static final MariaDBContainer<?> MARIADB =
      new MariaDBContainer<>("mariadb:11.4")
          .withNetwork(network)
          .withDatabaseName("pre_process")
          .withUsername("mariadb")
          .withPassword("qwer1234");

  private static final GenericContainer<?> LOKI =
      new GenericContainer<>(DockerImageName.parse("grafana/loki:2.9.0"))
          .withNetwork(network)
          .withNetworkAliases("loki")
          .withExposedPorts(3100)
          .withCommand("-config.file=/etc/loki/local-config.yaml")
          .waitingFor(Wait.forHttp("/ready"))
          .withStartupTimeout(java.time.Duration.ofMinutes(2));

  static {
    MARIADB.start();
    LOKI.start();

    // Log4j2에서 사용할 시스템 프로퍼티 설정
    System.setProperty("loki-port", String.valueOf(LOKI.getMappedPort(3100)));
    System.setProperty(
        "DriverManager.connectionString", MARIADB.getJdbcUrl() + "?serverTimezone=UTC");
    System.setProperty("DriverManager.driverClassName", "org.mariadb.jdbc.Driver");
    System.setProperty("DriverManager.userName", MARIADB.getUsername());
    System.setProperty("DriverManager.password", MARIADB.getPassword());
  }

  @Bean
  @ServiceConnection
  MariaDBContainer<?> mariadbContainer() {
    return MARIADB;
  }

  @Bean
  GenericContainer<?> lokiContainer() {
    return LOKI;
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // HikariCP 설정
    registry.add("spring.hikari.connection-timeout", () -> "30000");
    registry.add("spring.hikari.idle-timeout", () -> "600000");
    registry.add("spring.hikari.max-lifetime", () -> "1800000");
    registry.add("spring.hikari.maximum-pool-size", () -> "10");
    registry.add("spring.hikari.minimum-idle", () -> "5");
    registry.add("spring.hikari.pool-name", () -> "HikariCP-E2E");

    registry.add("loki.port", () -> LOKI.getMappedPort(3100));
  }
}
