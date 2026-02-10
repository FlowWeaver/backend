package site.icebang.external.fastapi.adapter;

import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import site.icebang.global.config.properties.FastApiProperties;

/**
 * 외부 FastAPI 서버와의 모든 HTTP 통신을 전담하는 어댑터 클래스입니다.
 *
 * <p>이 클래스는 내부 시스템의 다른 부분들이 외부 시스템의 상세한 통신 방법을 알 필요가 없도록 HTTP 요청/응답 로직을 캡슐화합니다. {@code RestClient}을
 * 사용하여 실제 통신을 수행하며, 모든 FastAPI 요청은 이 클래스의 {@code call} 메소드를 통해 이루어져야 합니다.
 *
 * <h2>사용 예제:</h2>
 *
 * <pre>{@code
 * @Autowired
 * private FastApiAdapter fastApiAdapter;
 *
 * String response = fastApiAdapter.call("/keywords/search", HttpMethod.POST, "{\"tag\":\"naver\"}");
 * }</pre>
 *
 * @author jihu0210@naver.com
 * @since v0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiAdapter {

  private final RestClient restClient;
  private final FastApiProperties properties;

  /**
   * FastAPI 서버에 API 요청을 보내는 범용 메소드입니다.
   *
   * <p>지정된 엔드포인트, HTTP 메소드, 요청 Body를 사용하여 외부 API를 호출합니다. 통신 성공 시 응답 Body를 문자열로 반환하고, 실패 시 에러 로그를
   * 남기고 null을 반환합니다.
   *
   * @param endpoint 호출할 엔드포인트 경로 (예: "/keywords/search")
   * @param method 사용할 HTTP 메소드 (예: HttpMethod.POST)
   * @param requestBody 요청에 담을 JSON 문자열
   * @return 성공 시 API 응답 Body 문자열, 실패 시 null
   * @see RestClient
   * @since v0.1.0
   */
  public String call(String endpoint, HttpMethod method, String requestBody) {
    String fullUrl = properties.getUrl() + endpoint;

    try {
      log.debug("FastAPI 요청: URL={}, Method={}, Body={}", fullUrl, method, requestBody);

      ResponseEntity<String> responseEntity =
          restClient
              .method(method)
              .uri(fullUrl)
              .contentType(MediaType.APPLICATION_JSON)
              .headers(
                  headers -> {
                    String traceId = MDC.get("traceId");
                    if (traceId != null) {
                      headers.set("X-Request-ID", traceId);
                      log.debug("TraceID 헤더 추가: {}", traceId);
                    }
                  })
              .body(requestBody)
              .retrieve()
              .toEntity(String.class);

      String responseBody = responseEntity.getBody();
      log.debug("FastAPI 응답: Status={}, Body={}", responseEntity.getStatusCode(), responseBody);
      return responseBody;
    } catch (RestClientException e) {
      log.error("FastAPI 호출 실패: URL={}, Error={}", fullUrl, e.getMessage());
      return null;
    }
  }
}
