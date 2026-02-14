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

  private static Network network;
  private static MariaDBContainer<?> MARIADB;
  private static GenericContainer<?> LOKI;

  static {
    try {
      // 1. CI 환경 안정성을 위한 설정
      System.setProperty("testcontainers.ryuk.disabled", "true");
      if (System.getProperty("os.name").toLowerCase().contains("linux")) {
        // Docker 소켓 경로 명시 (CI 환경에서 못 찾는 경우 대비)
        System.setProperty("DOCKER_HOST", "unix:///var/run/docker.sock");
      }

      // 2. 리소스 초기화
      network = Network.newNetwork();

      MARIADB =
          new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
              .withNetwork(network)
              .withDatabaseName("pre_process")
              .withUsername("mariadb")
              .withPassword("qwer1234");

      LOKI =
          new GenericContainer<>(DockerImageName.parse("grafana/loki:2.9.0"))
              .withNetwork(network)
              .withNetworkAliases("loki")
              .withExposedPorts(3100)
              .withCommand("-config.file=/etc/loki/local-config.yaml")
              .waitingFor(Wait.forHttp("/ready"))
              .withStartupTimeout(java.time.Duration.ofMinutes(2));

      // 3. 컨테이너 시작
      MARIADB.start();
      LOKI.start();

      // 4. Log4j2 및 시스템 전역에서 사용할 프로퍼티 설정
      String lokiPort = String.valueOf(LOKI.getMappedPort(3100));
      System.setProperty("loki-port", lokiPort);
      System.setProperty("loki.port", lokiPort);

      System.setProperty(
          "DriverManager.connectionString", MARIADB.getJdbcUrl() + "?serverTimezone=UTC");
      System.setProperty("DriverManager.driverClassName", "org.mariadb.jdbc.Driver");
      System.setProperty("DriverManager.userName", MARIADB.getUsername());
      System.setProperty("DriverManager.password", MARIADB.getPassword());

    } catch (Exception e) {
      System.err.println("CRITICAL: Failed to initialize Testcontainers - " + e.getMessage());
      e.printStackTrace();
    }
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
    // HikariCP 및 Loki 설정 주입
    registry.add("spring.hikari.connection-timeout", () -> "30000");
    registry.add("spring.hikari.idle-timeout", () -> "600000");
    registry.add("spring.hikari.max-lifetime", () -> "1800000");
    registry.add("spring.hikari.maximum-pool-size", () -> "10");
    registry.add("spring.hikari.minimum-idle", () -> "5");
    registry.add("spring.hikari.pool-name", () -> "HikariCP-E2E");

    registry.add("loki.port", () -> LOKI.getMappedPort(3100));
  }
}
