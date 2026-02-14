package site.icebang.common.health.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import site.icebang.global.config.properties.FastApiProperties;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

  private final RestTemplate restTemplate;

  private final FastApiProperties fastApiProperties;

  /** FastAPI 서버의 /ping 엔드포인트를 호출하여 연결을 테스트합니다. */
  public String ping() {
    String url = fastApiProperties.getUrl() + "/ping";
    log.info("Attempting to connect to FastAPI server at: {}", url);

    try {
      return restTemplate.getForObject(url, String.class);
    } catch (RestClientException e) {
      log.error("Failed to connect to FastAPI server at {}. Error: {}", url, e.getMessage());
      return "ERROR: Cannot connect to FastAPI";
    }
  }
}
