package site.icebang.e2e.scenario;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;

import site.icebang.e2e.setup.annotation.E2eTest;
import site.icebang.e2e.setup.support.E2eTestSupport;

@Sql(
    value = {
      "classpath:sql/data/00-truncate.sql",
      "classpath:sql/data/01-insert-internal-users.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DisplayName("사용자 로그아웃 플로우 E2E 테스트")
@E2eTest
class UserLogoutFlowE2eTest extends E2eTestSupport {

  @SuppressWarnings("unchecked")
  @Disabled
  @DisplayName("정상 로그아웃 전체 플로우 - TDD Red 단계")
  void completeUserLogoutFlow_shouldFailBecauseApiNotImplemented() throws Exception {
    logStep(1, "관리자 로그인 (최우선)");

    Map<String, String> loginRequest = new HashMap<>();
    loginRequest.put("email", "admin@icebang.site");
    loginRequest.put("password", "qwer1234!A");

    HttpHeaders loginHeaders = new HttpHeaders();
    loginHeaders.setContentType(MediaType.APPLICATION_JSON);
    loginHeaders.set("Origin", "https://admin.icebang.site");
    loginHeaders.set("Referer", "https://admin.icebang.site/");

    ResponseEntity<Map> loginResponse =
        restClient
            .post()
            .uri(getV0ApiUrl("/auth/login"))
            .headers(h -> h.addAll(loginHeaders))
            .body(loginRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Boolean) loginResponse.getBody().get("success")).isTrue();

    logSuccess("관리자 로그인 성공 - 세션 쿠키 자동 저장됨");
    logDebug("현재 세션 쿠키: " + getSessionCookies());

    logStep(2, "로그인 상태에서 보호된 리소스 접근 확인");

    ResponseEntity<Map> beforeLogoutResponse =
        restClient
            .get()
            .uri(getV0ApiUrl("/users/me"))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(beforeLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Boolean) beforeLogoutResponse.getBody().get("success")).isTrue();
    assertThat(beforeLogoutResponse.getBody().get("data")).isNotNull();

    logSuccess("인증된 상태에서 본인 프로필 조회 성공");

    logStep(3, "로그아웃 API 호출");

    HttpHeaders logoutHeaders = new HttpHeaders();
    logoutHeaders.setContentType(MediaType.APPLICATION_JSON);
    logoutHeaders.set("Origin", "https://admin.icebang.site");
    logoutHeaders.set("Referer", "https://admin.icebang.site/");

    try {
      ResponseEntity<Map> logoutResponse =
          restClient
              .post()
              .uri(getV0ApiUrl("/auth/logout"))
              .headers(h -> h.addAll(logoutHeaders))
              .body(new HashMap<>())
              .retrieve()
              .onStatus(HttpStatusCode::isError, (req, res) -> {})
              .toEntity(Map.class);

      logStep(4, "로그아웃 응답 검증");
      assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat((Boolean) logoutResponse.getBody().get("success")).isTrue();

      logSuccess("로그아웃 API 호출 성공");

      logStep(5, "로그아웃 후 인증 무효화 확인");
      logDebug("로그아웃 후 세션 쿠키: " + getSessionCookies());

      ResponseEntity<Map> afterLogoutResponse =
          restClient
              .get()
              .uri(getV0ApiUrl("/users/me"))
              .retrieve()
              .onStatus(HttpStatusCode::isError, (req, res) -> {})
              .toEntity(Map.class);

      assertThat(afterLogoutResponse.getStatusCode())
          .withFailMessage(
              "로그아웃 후 프로필 접근이 차단되어야 합니다. 현재 상태코드: %s", afterLogoutResponse.getStatusCode())
          .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

      logSuccess("로그아웃 후 프로필 접근 차단 확인 - 인증 무효화 성공");
      logCompletion("관리자 로그아웃 플로우");

    } catch (Exception ex) {
      logError("로그아웃 API 호출 중 예외 발생: " + ex.getMessage());
      fail("로그아웃 API 호출 실패: " + ex.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  @DisplayName("일반 사용자 로그아웃 플로우 테스트")
  void regularUserLogoutFlow() throws Exception {
    logStep(1, "일반 사용자 로그인");
    clearSessionCookies();
    performRegularUserLogin();

    logStep(2, "일반 사용자 권한으로 프로필 조회");
    ResponseEntity<Map> beforeLogoutResponse =
        restClient
            .get()
            .uri(getV0ApiUrl("/users/me"))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(beforeLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Boolean) beforeLogoutResponse.getBody().get("success")).isTrue();

    logSuccess("일반 사용자 프로필 조회 성공");

    logStep(3, "일반 사용자 로그아웃 시도");
    try {
      HttpHeaders logoutHeaders = new HttpHeaders();
      logoutHeaders.setContentType(MediaType.APPLICATION_JSON);
      logoutHeaders.set("Origin", "https://admin.icebang.site");
      logoutHeaders.set("Referer", "https://admin.icebang.site/");

      ResponseEntity<Map> logoutResponse =
          restClient
              .post()
              .uri(getV0ApiUrl("/auth/logout"))
              .headers(h -> h.addAll(logoutHeaders))
              .body(new HashMap<>())
              .retrieve()
              .onStatus(HttpStatusCode::isError, (req, res) -> {})
              .toEntity(Map.class);

      assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
      logSuccess("일반 사용자 로그아웃 성공");

      logStep(4, "로그아웃 후 접근 권한 무효화 확인");
      ResponseEntity<Map> afterLogoutResponse =
          restClient
              .get()
              .uri(getV0ApiUrl("/users/me"))
              .retrieve()
              .onStatus(HttpStatusCode::isError, (req, res) -> {})
              .toEntity(Map.class);

      assertThat(afterLogoutResponse.getStatusCode())
          .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

      logSuccess("일반 사용자 로그아웃 후 접근 차단 확인");
      logCompletion("일반 사용자 로그아웃 플로우");

    } catch (Exception ex) {
      logError("로그아웃 API 오류: " + ex.getMessage());
      fail("로그아웃 API 호출 중 오류 발생: " + ex.getMessage());
    }
  }

  private void performRegularUserLogin() {
    Map<String, String> loginRequest = new HashMap<>();
    loginRequest.put("email", "admin@icebang.site");
    loginRequest.put("password", "qwer1234!A");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Origin", "https://admin.icebang.site");
    headers.set("Referer", "https://admin.icebang.site/");

    ResponseEntity<Map> response =
        restClient
            .post()
            .uri(getV0ApiUrl("/auth/login"))
            .headers(h -> h.addAll(headers))
            .body(loginRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    if (response.getStatusCode() != HttpStatus.OK) {
      logError("일반 사용자 로그인 실패: " + response.getStatusCode());
      throw new RuntimeException("Regular user login failed for logout test");
    }

    logSuccess("일반 사용자 로그인 완료 - 세션 쿠키 저장됨");
    logDebug("일반 사용자 세션 쿠키: " + getSessionCookies());
  }
}
