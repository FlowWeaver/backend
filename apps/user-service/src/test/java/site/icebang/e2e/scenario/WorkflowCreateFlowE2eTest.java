package site.icebang.e2e.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
@DisplayName("워크플로우 생성 플로우 E2E 테스트")
@E2eTest
class WorkflowCreateFlowE2eTest extends E2eTestSupport {

  @SuppressWarnings("unchecked")
  @Test
  @DisplayName("사용자가 새 워크플로우를 생성하는 전체 플로우")
  void completeWorkflowCreateFlow() throws Exception {
    logStep(1, "사용자 로그인");

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

    logSuccess("사용자 로그인 성공 - 세션 쿠키 자동 저장됨");
    logDebug("현재 세션 쿠키: " + getSessionCookies());

    logStep(2, "네이버 블로그 워크플로우 생성");

    Map<String, Object> naverBlogWorkflow = new HashMap<>();
    naverBlogWorkflow.put("name", "상품 분석 및 네이버 블로그 자동 발행");
    naverBlogWorkflow.put("description", "키워드 검색부터 상품 분석 후 네이버 블로그 발행까지의 자동화 프로세스");
    naverBlogWorkflow.put("search_platform", "naver");
    naverBlogWorkflow.put("posting_platform", "naver_blog");
    naverBlogWorkflow.put("posting_account_id", "test_naver_blog");
    naverBlogWorkflow.put("posting_account_password", "naver_password123");
    naverBlogWorkflow.put("is_enabled", true);

    HttpHeaders workflowHeaders = new HttpHeaders();
    workflowHeaders.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> naverResponse =
        restClient
            .post()
            .uri(getV0ApiUrl("/workflows"))
            .headers(h -> h.addAll(workflowHeaders))
            .body(naverBlogWorkflow)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(naverResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) naverResponse.getBody().get("success")).isTrue();

    logSuccess("네이버 블로그 워크플로우 생성 성공");

    logStep(3, "티스토리 블로그 워크플로우 생성 (블로그명 포함)");

    Map<String, Object> tstoryWorkflow = new HashMap<>();
    tstoryWorkflow.put("name", "티스토리 자동 발행 워크플로우");
    tstoryWorkflow.put("description", "티스토리 블로그 자동 포스팅");
    tstoryWorkflow.put("search_platform", "naver");
    tstoryWorkflow.put("posting_platform", "tstory_blog");
    tstoryWorkflow.put("posting_account_id", "test_tstory");
    tstoryWorkflow.put("posting_account_password", "tstory_password123");
    tstoryWorkflow.put("blog_name", "my-tech-blog");
    tstoryWorkflow.put("is_enabled", true);

    ResponseEntity<Map> tstoryResponse =
        restClient
            .post()
            .uri(getV0ApiUrl("/workflows"))
            .headers(h -> h.addAll(workflowHeaders))
            .body(tstoryWorkflow)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(tstoryResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) tstoryResponse.getBody().get("success")).isTrue();

    logSuccess("티스토리 워크플로우 생성 성공");

    logStep(4, "검색만 하는 워크플로우 생성 (포스팅 없음)");

    Map<String, Object> searchOnlyWorkflow = new HashMap<>();
    searchOnlyWorkflow.put("name", "검색 전용 워크플로우");
    searchOnlyWorkflow.put("description", "상품 검색 및 분석만 수행");
    searchOnlyWorkflow.put("search_platform", "naver");
    searchOnlyWorkflow.put("is_enabled", true);

    ResponseEntity<Map> searchOnlyResponse =
        restClient
            .post()
            .uri(getV0ApiUrl("/workflows"))
            .headers(h -> h.addAll(workflowHeaders))
            .body(searchOnlyWorkflow)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(searchOnlyResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) searchOnlyResponse.getBody().get("success")).isTrue();

    logSuccess("검색 전용 워크플로우 생성 성공");
    logCompletion("워크플로우 생성 플로우 완료");
  }

  @Test
  @DisplayName("중복된 이름으로 워크플로우 생성 시도 시 실패")
  void createWorkflow_withDuplicateName_shouldFail() {
    performUserLogin();

    logStep(1, "첫 번째 워크플로우 생성");
    Map<String, Object> firstWorkflow = new HashMap<>();
    firstWorkflow.put("name", "중복테스트워크플로우");
    firstWorkflow.put("search_platform", "naver");
    firstWorkflow.put("is_enabled", true);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> firstResponse =
        restClient
            .post()
            .uri(getV0ApiUrl("/workflows"))
            .headers(h -> h.addAll(headers))
            .body(firstWorkflow)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    logSuccess("첫 번째 워크플로우 생성 성공");

    logStep(2, "동일한 이름으로 두 번째 워크플로우 생성 시도");
    Map<String, Object> duplicateWorkflow = new HashMap<>();
    duplicateWorkflow.put("name", "중복테스트워크플로우");
    duplicateWorkflow.put("search_platform", "naver_store");
    duplicateWorkflow.put("is_enabled", true);

    ResponseEntity<Map> duplicateResponse =
        restClient
            .post()
            .uri(getV0ApiUrl("/workflows"))
            .headers(h -> h.addAll(headers))
            .body(duplicateWorkflow)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(duplicateResponse.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.CONFLICT, HttpStatus.INTERNAL_SERVER_ERROR);
    logSuccess("중복 이름 워크플로우 생성 차단 확인");
  }

  @Test
  @DisplayName("필수 필드 누락 시 워크플로우 생성 실패")
  void createWorkflow_withMissingRequiredFields_shouldFail() {
    performUserLogin();

    logStep(1, "워크플로우 이름 없이 생성 시도");
    Map<String, Object> noNameWorkflow = new HashMap<>();
    noNameWorkflow.put("search_platform", "naver");
    noNameWorkflow.put("is_enabled", true);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> response =
        restClient
            .post()
            .uri(getV0ApiUrl("/workflows"))
            .headers(h -> h.addAll(headers))
            .body(noNameWorkflow)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(response.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNPROCESSABLE_ENTITY);
    logSuccess("필수 필드 검증 확인");
  }

  private void performUserLogin() {
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
      logError("사용자 로그인 실패: " + response.getStatusCode());
      throw new RuntimeException("User login failed");
    }

    logSuccess("사용자 로그인 완료");
  }

  @Test
  @DisplayName("워크플로우 생성 시 UTC 시간 기반으로 생성 시간이 저장되는지 검증")
  void createWorkflow_utc_time_validation() throws Exception {
    performUserLogin();

    logStep(3, "워크플로우 생성");
    Map<String, Object> workflowRequest = new HashMap<>();
    workflowRequest.put("name", "UTC 시간 검증 워크플로우");
    workflowRequest.put("description", "UTC 시간대 보장을 위한 테스트 워크플로우");
    workflowRequest.put("search_platform", "naver");
    workflowRequest.put("is_enabled", true);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> createResponse =
        restClient
            .post()
            .uri(getV0ApiUrl("/workflows"))
            .headers(h -> h.addAll(headers))
            .body(workflowRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    logStep(5, "생성된 워크플로우 목록 조회하여 시간 검증");
    ResponseEntity<Map> listResponse =
        restClient
            .get()
            .uri(getV0ApiUrl("/workflows"))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    Map<String, Object> data = (Map<String, Object>) listResponse.getBody().get("data");
    java.util.List<Map<String, Object>> workflows =
        (java.util.List<Map<String, Object>>) data.get("data");

    Map<String, Object> createdWorkflow =
        workflows.stream()
            .filter(w -> "UTC 시간 검증 워크플로우".equals(w.get("name")))
            .findFirst()
            .orElseThrow();

    String createdAtStr = (String) createdWorkflow.get("createdAt");
    assertThat(createdAtStr).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
    logSuccess("워크플로우가 UTC 시간 기준으로 생성됨을 확인");
  }

  @Test
  @DisplayName("워크플로우 생성 시 단일 스케줄 등록 성공")
  void createWorkflow_withSingleSchedule_success() {
    performUserLogin();

    logStep(1, "스케줄이 포함된 워크플로우 생성");
    Map<String, Object> workflowRequest = new HashMap<>();
    workflowRequest.put("name", "매일 오전 9시 자동 실행 워크플로우");
    workflowRequest.put("search_platform", "naver");
    workflowRequest.put("is_enabled", true);

    List<Map<String, Object>> schedules = new ArrayList<>();
    Map<String, Object> schedule = new HashMap<>();
    schedule.put("cronExpression", "0 0 9 * * ?");
    schedule.put("scheduleText", "매일 오전 9시");
    schedule.put("isActive", true);
    schedules.add(schedule);
    workflowRequest.put("schedules", schedules);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> response =
        restClient
            .post()
            .uri(getV0ApiUrl("/workflows"))
            .headers(h -> h.addAll(headers))
            .body(workflowRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    logSuccess("스케줄이 포함된 워크플로우 생성 성공");
  }
}
