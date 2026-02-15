package site.icebang.e2e.setup.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import site.icebang.e2e.setup.annotation.E2eTest;
import site.icebang.e2e.setup.config.E2eTestConfiguration;

@Import(E2eTestConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@E2eTest
public abstract class E2eTestSupport {
  @Autowired protected TestRestTemplate restTemplate;

  @Autowired protected ObjectMapper objectMapper;

  @LocalServerPort protected int port;

  @Autowired protected WebApplicationContext webApplicationContext;

  protected MockMvc mockMvc;

  private List<String> sessionCookies = new ArrayList<>();

  @PostConstruct
  void setupCookieManagement() {
    // RestTemplate에 쿠키 인터셉터 추가
    restTemplate.getRestTemplate().getInterceptors().add(createCookieInterceptor());
    logDebug("쿠키 관리 인터셉터 설정 완료");
  }

  private ClientHttpRequestInterceptor createCookieInterceptor() {
    return (request, body, execution) -> {
      // 요청에 저장된 쿠키 추가
      if (!sessionCookies.isEmpty()) {
        request.getHeaders().put("Cookie", sessionCookies);
        logDebug("쿠키 전송: " + String.join("; ", sessionCookies));
      }

      // 요청 실행
      ClientHttpResponse response = execution.execute(request, body);

      // 응답에서 Set-Cookie 헤더 추출하여 저장
      List<String> setCookieHeaders = response.getHeaders().get("Set-Cookie");
      if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
        updateSessionCookies(setCookieHeaders);
        logDebug("세션 쿠키 업데이트: " + String.join("; ", sessionCookies));
      }

      return response;
    };
  }

  private void updateSessionCookies(List<String> setCookieHeaders) {
    for (String setCookie : setCookieHeaders) {
      // 쿠키 이름 추출
      String cookieName = setCookie.split("=")[0];
      String cookieValue = setCookie.split(";")[0]; // 쿠키 값만 추출 (속성 제외)

      // 같은 이름의 쿠키가 있으면 제거
      sessionCookies.removeIf(cookie -> cookie.startsWith(cookieName + "="));

      // 새 쿠키 추가 (빈 값이 아닌 경우만)
      if (!cookieValue.endsWith("=")) {
        sessionCookies.add(cookieValue);
      }
    }
  }

  protected String getBaseUrl() {
    return "http://localhost:" + port;
  }

  protected String getApiUrl(String path) {
    return getBaseUrl() + path;
  }

  protected String getV0ApiUrl(String path) {
    return getBaseUrl() + "/v0" + path;
  }

  /** 세션 쿠키 관리 메서드들 */
  protected void clearSessionCookies() {
    sessionCookies.clear();
    logDebug("세션 쿠키 초기화됨");
  }

  protected List<String> getSessionCookies() {
    return new ArrayList<>(sessionCookies);
  }

  protected boolean hasSessionCookie(String cookieName) {
    return sessionCookies.stream().anyMatch(cookie -> cookie.startsWith(cookieName + "="));
  }

  /** 테스트 시나리오 단계별 로깅을 위한 유틸리티 메서드 */
  protected void logStep(int stepNumber, String description) {
    System.out.println(String.format("📋 Step %d: %s", stepNumber, description));
  }

  /** 테스트 성공 로깅을 위한 유틸리티 메서드 */
  protected void logSuccess(String message) {
    System.out.println("✅ " + message);
  }

  /** 테스트 실패 로깅을 위한 유틸리티 메서드 */
  protected void logError(String message) {
    System.out.println("❌ " + message);
  }

  /** 테스트 완료 로깅을 위한 유틸리티 메서드 */
  protected void logCompletion(String scenario) {
    System.out.println(String.format("🎉 %s 시나리오 완료!", scenario));
  }

  /** 디버그 로깅을 위한 유틸리티 메서드 */
  protected void logDebug(String message) {
    if (isDebugEnabled()) {
      System.out.println("🐛 DEBUG: " + message);
    }
  }

  private boolean isDebugEnabled() {
    return System.getProperty("test.debug", "false").equals("true")
        || System.getProperty("e2e.debug", "false").equals("true");
  }
}
