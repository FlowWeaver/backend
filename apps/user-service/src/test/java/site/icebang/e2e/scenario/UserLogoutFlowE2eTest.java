package site.icebang.e2e.scenario;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
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

    // 1. 관리자 로그인으로 인증 상태 확립
    Map<String, String> loginRequest = new HashMap<>();
    loginRequest.put("email", "admin@icebang.site");
    loginRequest.put("password", "qwer1234!A");

    HttpHeaders loginHeaders = new HttpHeaders();
    loginHeaders.setContentType(MediaType.APPLICATION_JSON);
    loginHeaders.set("Origin", "https://admin.icebang.site");
    loginHeaders.set("Referer", "https://admin.icebang.site/");

    HttpEntity<Map<String, String>> loginEntity = new HttpEntity<>(loginRequest, loginHeaders);

    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(getV0ApiUrl("/auth/login"), loginEntity, Map.class);

    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Boolean) loginResponse.getBody().get("success")).isTrue();

    logSuccess("관리자 로그인 성공 - 세션 쿠키 자동 저장됨");
    logDebug("현재 세션 쿠키: " + getSessionCookies());

    logStep(2, "로그인 상태에서 보호된 리소스 접근 확인");

    // 2. 로그인된 상태에서 본인 프로필 조회로 인증 상태 확인
    // /v0/users/me는 인증된 사용자만 접근 가능한 일반적인 API
    // 쿠키는 인터셉터에 의해 자동으로 전송됨
    ResponseEntity<Map> beforeLogoutResponse =
        restTemplate.getForEntity(getV0ApiUrl("/users/me"), Map.class);

    assertThat(beforeLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Boolean) beforeLogoutResponse.getBody().get("success")).isTrue();
    assertThat(beforeLogoutResponse.getBody().get("data")).isNotNull();

    logSuccess("인증된 상태에서 본인 프로필 조회 성공");

    logStep(3, "로그아웃 API 호출");

    // 3. 로그아웃 API 호출 (세션 쿠키는 인터셉터가 자동 처리)
    HttpHeaders logoutHeaders = new HttpHeaders();
    logoutHeaders.setContentType(MediaType.APPLICATION_JSON);
    logoutHeaders.set("Origin", "https://admin.icebang.site");
    logoutHeaders.set("Referer", "https://admin.icebang.site/");

    HttpEntity<Map<String, Object>> logoutEntity = new HttpEntity<>(new HashMap<>(), logoutHeaders);

    try {
      ResponseEntity<Map> logoutResponse =
          restTemplate.postForEntity(getV0ApiUrl("/auth/logout"), logoutEntity, Map.class);

      logStep(4, "로그아웃 응답 검증");
      assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat((Boolean) logoutResponse.getBody().get("success")).isTrue();

      logSuccess("로그아웃 API 호출 성공");

      logStep(5, "로그아웃 후 인증 무효화 확인");

      // 로그아웃 후 세션 쿠키 상태 확인
      logDebug("로그아웃 후 세션 쿠키: " + getSessionCookies());

      // 5. 로그아웃 후 동일한 프로필 API 접근 시 인증 실패 확인
      ResponseEntity<Map> afterLogoutResponse =
          restTemplate.getForEntity(getV0ApiUrl("/users/me"), Map.class);

      // 핵심 검증: 로그아웃 후에는 인증 실패로 401 또는 403 응답이어야 함
      assertThat(afterLogoutResponse.getStatusCode())
          .withFailMessage(
              "로그아웃 후 프로필 접근이 차단되어야 합니다. 현재 상태코드: %s", afterLogoutResponse.getStatusCode())
          .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

      logSuccess("로그아웃 후 프로필 접근 차단 확인 - 인증 무효화 성공");
      logCompletion("관리자 로그아웃 플로우");

    } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
      logError("예상된 실패: 로그아웃 API가 구현되지 않음 (404 Not Found)");
      logError("에러 메시지: " + ex.getMessage());
      logError("TDD Red 단계 - API 구현 필요");

      fail(
          "로그아웃 API (/v0/auth/logout)가 구현되지 않았습니다. "
              + "다음 단계에서 API를 구현해야 합니다. 에러: "
              + ex.getMessage());

    } catch (org.springframework.web.client.HttpClientErrorException ex) {
      logError("HTTP 클라이언트 에러: " + ex.getStatusCode() + " - " + ex.getMessage());

      if (ex.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED) {
        logError("로그아웃 엔드포인트는 존재하지만 POST 메서드를 지원하지 않습니다.");
        fail("로그아웃 API가 POST 메서드를 지원하지 않습니다. 구현을 확인해주세요.");
      } else {
        fail("로그아웃 API 호출 중 HTTP 에러 발생: " + ex.getStatusCode() + " - " + ex.getMessage());
      }

    } catch (Exception ex) {
      logError("예상치 못한 오류 발생: " + ex.getClass().getSimpleName());
      logError("에러 메시지: " + ex.getMessage());

      // 기타 예상치 못한 에러도 기록
      fail("로그아웃 API 호출 중 예상치 못한 오류 발생: " + ex.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  @DisplayName("일반 사용자 로그아웃 플로우 테스트")
  void regularUserLogoutFlow() throws Exception {
    logStep(1, "일반 사용자 로그인");

    // 세션 쿠키 초기화
    clearSessionCookies();

    // 일반 사용자 로그인 수행
    performRegularUserLogin();

    logStep(2, "일반 사용자 권한으로 프로필 조회");

    // 로그인된 상태에서 프로필 조회
    ResponseEntity<Map> beforeLogoutResponse =
        restTemplate.getForEntity(getV0ApiUrl("/users/me"), Map.class);

    assertThat(beforeLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Boolean) beforeLogoutResponse.getBody().get("success")).isTrue();

    logSuccess("일반 사용자 프로필 조회 성공");

    logStep(3, "일반 사용자 로그아웃 시도");

    try {
      HttpHeaders logoutHeaders = new HttpHeaders();
      logoutHeaders.setContentType(MediaType.APPLICATION_JSON);
      logoutHeaders.set("Origin", "https://admin.icebang.site");
      logoutHeaders.set("Referer", "https://admin.icebang.site/");

      HttpEntity<Map<String, Object>> logoutEntity =
          new HttpEntity<>(new HashMap<>(), logoutHeaders);

      ResponseEntity<Map> logoutResponse =
          restTemplate.postForEntity(getV0ApiUrl("/auth/logout"), logoutEntity, Map.class);

      assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
      logSuccess("일반 사용자 로그아웃 성공");

      logStep(4, "로그아웃 후 접근 권한 무효화 확인");

      ResponseEntity<Map> afterLogoutResponse =
          restTemplate.getForEntity(getV0ApiUrl("/users/me"), Map.class);

      assertThat(afterLogoutResponse.getStatusCode())
          .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

      logSuccess("일반 사용자 로그아웃 후 접근 차단 확인");
      logCompletion("일반 사용자 로그아웃 플로우");

    } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
      logError("예상된 실패: 로그아웃 API 미구현");
      fail("로그아웃 API가 구현되지 않았습니다: " + ex.getMessage());
    }
  }

  /** 일반 사용자 로그인을 수행하는 헬퍼 메서드 - 관리자가 아닌 콘텐츠팀장으로 로그인 */
  private void performRegularUserLogin() {
    Map<String, String> loginRequest = new HashMap<>();
    loginRequest.put("email", "viral.jung@icebang.site");
    loginRequest.put("password", "qwer1234!A"); // 실제 비밀번호 확인 필요

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Origin", "https://admin.icebang.site");
    headers.set("Referer", "https://admin.icebang.site/");

    HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);

    ResponseEntity<Map> response =
        restTemplate.postForEntity(getV0ApiUrl("/auth/login"), entity, Map.class);

    if (response.getStatusCode() != HttpStatus.OK) {
      logError("일반 사용자 로그인 실패: " + response.getStatusCode());
      throw new RuntimeException("Regular user login failed for logout test");
    }

    logSuccess("일반 사용자 로그인 완료 - 세션 쿠키 저장됨");
    logDebug("일반 사용자 세션 쿠키: " + getSessionCookies());
  }

  /** 관리자 로그인을 수행하는 헬퍼 메서드 */
  private void performAdminLogin() {
    clearSessionCookies(); // 기존 세션 정리

    Map<String, String> loginRequest = new HashMap<>();
    loginRequest.put("email", "admin@icebang.site");
    loginRequest.put("password", "qwer1234!A");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Origin", "https://admin.icebang.site");
    headers.set("Referer", "https://admin.icebang.site/");

    HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);

    ResponseEntity<Map> response =
        restTemplate.postForEntity(getV0ApiUrl("/auth/login"), entity, Map.class);

    if (response.getStatusCode() != HttpStatus.OK) {
      logError("관리자 로그인 실패: " + response.getStatusCode());
      throw new RuntimeException("Admin login failed");
    }

    logSuccess("관리자 로그인 완료 - 세션 쿠키 저장됨");
    logDebug("관리자 세션 쿠키: " + getSessionCookies());
  }
}
