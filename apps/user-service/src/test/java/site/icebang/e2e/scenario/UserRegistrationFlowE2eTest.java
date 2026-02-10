package site.icebang.e2e.scenario;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
@DisplayName("사용자 등록 플로우 E2E 테스트")
@E2eTest
class UserRegistrationFlowE2eTest extends E2eTestSupport {

  @SuppressWarnings("unchecked")
  @Test
  @DisplayName("관리자가 새 사용자를 등록하는 전체 플로우 (ERP 시나리오)")
  void completeUserRegistrationFlow() throws Exception {
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

    logStep(2, "조직 목록 조회 (인증된 상태)");

    ResponseEntity<Map> organizationsResponse =
        restClient
            .get()
            .uri(getV0ApiUrl("/organizations"))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(organizationsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Boolean) organizationsResponse.getBody().get("success")).isTrue();
    assertThat(organizationsResponse.getBody().get("data")).isNotNull();

    logSuccess("조직 목록 조회 성공 (인증된 요청)");

    logStep(3, "부서 및 각종 데이터 조회 (특정 조직 옵션)");

    ResponseEntity<Map> optionsResponse =
        restClient
            .get()
            .uri(getV0ApiUrl("/organizations/1/options"))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(optionsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Boolean) optionsResponse.getBody().get("success")).isTrue();

    Map<String, Object> optionData = (Map<String, Object>) optionsResponse.getBody().get("data");
    assertThat(optionData.get("departments")).isNotNull();
    assertThat(optionData.get("positions")).isNotNull();
    assertThat(optionData.get("roles")).isNotNull();

    logSuccess("부서 및 각종 데이터 조회 성공");

    System.out.println("📊 조회된 메타데이터:");
    System.out.println(
        "   - 부서: " + ((java.util.List<?>) optionData.get("departments")).size() + "개");
    System.out.println(
        "   - 직급: " + ((java.util.List<?>) optionData.get("positions")).size() + "개");
    System.out.println("   - 역할: " + ((java.util.List<?>) optionData.get("roles")).size() + "개");

    logStep(4, "새 사용자 등록 (모든 메타데이터 확인 후)");

    Map<String, Object> registerRequest = new HashMap<>();
    registerRequest.put("name", "김철수");
    registerRequest.put("email", "kim.chulsoo@example.com");
    registerRequest.put("orgId", 1);
    registerRequest.put("deptId", 2);
    registerRequest.put("positionId", 5);
    registerRequest.put("roleIds", Arrays.asList(6, 7, 8));
    registerRequest.put("password", null);

    HttpHeaders registerHeaders = new HttpHeaders();
    registerHeaders.setContentType(MediaType.APPLICATION_JSON);
    registerHeaders.set("Origin", "https://admin.icebang.site");
    registerHeaders.set("Referer", "https://admin.icebang.site/");

    ResponseEntity<Map> registerResponse =
        restClient
            .post()
            .uri(getV0ApiUrl("/auth/register"))
            .headers(h -> h.addAll(registerHeaders))
            .body(registerRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) registerResponse.getBody().get("success")).isTrue();

    logSuccess("새 사용자 등록 성공");
    logSuccess(
        "등록된 사용자: " + registerRequest.get("name") + " (" + registerRequest.get("email") + ")");

    logCompletion("ERP 사용자 등록 플로우");
  }

  @Test
  @DisplayName("잘못된 자격증명으로 로그인 시도 시 실패")
  void loginWithInvalidCredentials_shouldFail() {
    logStep(1, "잘못된 비밀번호로 로그인 시도");

    Map<String, String> wrongPasswordRequest = new HashMap<>();
    wrongPasswordRequest.put("email", "admin@icebang.site");
    wrongPasswordRequest.put("password", "wrongpassword");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> response =
        restClient
            .post()
            .uri(getV0ApiUrl("/auth/login"))
            .headers(h -> h.addAll(headers))
            .body(wrongPasswordRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    logSuccess("잘못된 자격증명 로그인 차단 확인");

    logStep(2, "존재하지 않는 사용자로 로그인 시도");

    Map<String, String> nonExistentUserRequest = new HashMap<>();
    nonExistentUserRequest.put("email", "nonexistent@example.com");
    nonExistentUserRequest.put("password", "anypassword");

    ResponseEntity<Map> nonExistentResponse =
        restClient
            .post()
            .uri(getV0ApiUrl("/auth/login"))
            .headers(h -> h.addAll(headers))
            .body(nonExistentUserRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(nonExistentResponse.getStatusCode())
        .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    logSuccess("존재하지 않는 사용자 로그인 차단 확인");
  }

  @SuppressWarnings("unchecked")
  @Test
  @DisplayName("중복 이메일로 사용자 등록 시도 시 실패")
  void register_withDuplicateEmail_shouldFail() {
    performAdminLogin();
    registerUser("first.user@example.com", "첫번째사용자");

    logStep(1, "중복 이메일로 회원가입 시도");

    ResponseEntity<Map> organizationsResponse =
        restClient
            .get()
            .uri(getV0ApiUrl("/organizations"))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);
    java.util.List<Map<String, Object>> organizations =
        (java.util.List<Map<String, Object>>) organizationsResponse.getBody().get("data");
    Integer orgId = (Integer) organizations.getFirst().get("id");

    ResponseEntity<Map> optionsResponse =
        restClient
            .get()
            .uri(getV0ApiUrl("/organizations/" + orgId + "/options"))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);
    Map<String, Object> optionData = (Map<String, Object>) optionsResponse.getBody().get("data");

    java.util.List<Map<String, Object>> departments =
        (java.util.List<Map<String, Object>>) optionData.get("departments");
    java.util.List<Map<String, Object>> positions =
        (java.util.List<Map<String, Object>>) optionData.get("positions");
    java.util.List<Map<String, Object>> roles =
        (java.util.List<Map<String, Object>>) optionData.get("roles");

    Integer deptId = (Integer) departments.getFirst().get("id");
    Integer positionId = (Integer) positions.getFirst().get("id");
    Integer roleId = (Integer) roles.getFirst().get("id");

    Map<String, Object> duplicateRequest = new HashMap<>();
    duplicateRequest.put("name", "중복사용자");
    duplicateRequest.put("email", "first.user@example.com");
    duplicateRequest.put("orgId", orgId);
    duplicateRequest.put("deptId", deptId);
    duplicateRequest.put("positionId", positionId);
    duplicateRequest.put("roleIds", Collections.singletonList(roleId));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> response =
        restClient
            .post()
            .uri(getV0ApiUrl("/auth/register"))
            .headers(h -> h.addAll(headers))
            .body(duplicateRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(response.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.CONFLICT, HttpStatus.UNPROCESSABLE_ENTITY);
    logSuccess("중복 이메일 등록 차단 확인");
  }

  private void performAdminLogin() {
    Map<String, String> loginRequest = new HashMap<>();
    loginRequest.put("email", "admin@icebang.site");
    loginRequest.put("password", "qwer1234!A");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

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
      logError("관리자 로그인 실패: " + response.getStatusCode());
      throw new RuntimeException("Admin login failed");
    }

    logSuccess("관리자 로그인 완료 - 세션 쿠키 저장됨");
    logDebug("세션 쿠키: " + getSessionCookies());
  }

  private void registerUser(String email, String name) {
    Map<String, Object> registerRequest = new HashMap<>();
    registerRequest.put("name", name);
    registerRequest.put("email", email);
    registerRequest.put("orgId", 1);
    registerRequest.put("deptId", 2);
    registerRequest.put("positionId", 5);
    registerRequest.put("roleIds", Arrays.asList(6, 7, 8));
    registerRequest.put("password", null);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    restClient
        .post()
        .uri(getV0ApiUrl("/auth/register"))
        .headers(h -> h.addAll(headers))
        .body(registerRequest)
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {})
        .toBodilessEntity();
  }
}
