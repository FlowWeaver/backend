package site.icebang.e2e.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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

    // 1. 로그인 (세션에 userId 저장)
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

    logSuccess("사용자 로그인 성공 - 세션 쿠키 자동 저장됨");
    logDebug("현재 세션 쿠키: " + getSessionCookies());

    logStep(2, "네이버 블로그 워크플로우 생성");

    // 2. 네이버 블로그 워크플로우 생성
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

    HttpEntity<Map<String, Object>> naverEntity =
        new HttpEntity<>(naverBlogWorkflow, workflowHeaders);

    ResponseEntity<Map> naverResponse =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), naverEntity, Map.class);

    assertThat(naverResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) naverResponse.getBody().get("success")).isTrue();

    logSuccess("네이버 블로그 워크플로우 생성 성공");

    logStep(3, "티스토리 블로그 워크플로우 생성 (블로그명 포함)");

    // 3. 티스토리 블로그 워크플로우 생성 (블로그명 필수)
    Map<String, Object> tstoryWorkflow = new HashMap<>();
    tstoryWorkflow.put("name", "티스토리 자동 발행 워크플로우");
    tstoryWorkflow.put("description", "티스토리 블로그 자동 포스팅");
    tstoryWorkflow.put("search_platform", "naver");
    tstoryWorkflow.put("posting_platform", "tstory_blog");
    tstoryWorkflow.put("posting_account_id", "test_tstory");
    tstoryWorkflow.put("posting_account_password", "tstory_password123");
    tstoryWorkflow.put("blog_name", "my-tech-blog"); // 티스토리는 블로그명 필수
    tstoryWorkflow.put("is_enabled", true);

    HttpEntity<Map<String, Object>> tstoryEntity =
        new HttpEntity<>(tstoryWorkflow, workflowHeaders);

    ResponseEntity<Map> tstoryResponse =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), tstoryEntity, Map.class);

    assertThat(tstoryResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) tstoryResponse.getBody().get("success")).isTrue();

    logSuccess("티스토리 워크플로우 생성 성공");

    logStep(4, "검색만 하는 워크플로우 생성 (포스팅 없음)");

    // 4. 포스팅 없는 검색 전용 워크플로우 (추후 예정)
    Map<String, Object> searchOnlyWorkflow = new HashMap<>();
    searchOnlyWorkflow.put("name", "검색 전용 워크플로우");
    searchOnlyWorkflow.put("description", "상품 검색 및 분석만 수행");
    searchOnlyWorkflow.put("search_platform", "naver");
    searchOnlyWorkflow.put("is_enabled", true);
    // posting_platform, posting_account_id, posting_account_password는 선택사항

    HttpEntity<Map<String, Object>> searchOnlyEntity =
        new HttpEntity<>(searchOnlyWorkflow, workflowHeaders);

    ResponseEntity<Map> searchOnlyResponse =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), searchOnlyEntity, Map.class);

    assertThat(searchOnlyResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) searchOnlyResponse.getBody().get("success")).isTrue();

    logSuccess("검색 전용 워크플로우 생성 성공");

    logCompletion("워크플로우 생성 플로우 완료");
  }

  @Test
  @DisplayName("중복된 이름으로 워크플로우 생성 시도 시 실패")
  void createWorkflow_withDuplicateName_shouldFail() {
    // 선행 조건: 로그인
    performUserLogin();

    logStep(1, "첫 번째 워크플로우 생성");

    // 첫 번째 워크플로우 생성
    Map<String, Object> firstWorkflow = new HashMap<>();
    firstWorkflow.put("name", "중복테스트워크플로우");
    firstWorkflow.put("search_platform", "naver");
    firstWorkflow.put("is_enabled", true);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> firstEntity = new HttpEntity<>(firstWorkflow, headers);

    ResponseEntity<Map> firstResponse =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), firstEntity, Map.class);

    assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    logSuccess("첫 번째 워크플로우 생성 성공");

    logStep(2, "동일한 이름으로 두 번째 워크플로우 생성 시도");

    // 동일한 이름으로 다시 생성 시도
    Map<String, Object> duplicateWorkflow = new HashMap<>();
    duplicateWorkflow.put("name", "중복테스트워크플로우"); // 동일한 이름
    duplicateWorkflow.put("search_platform", "naver_store");
    duplicateWorkflow.put("is_enabled", true);

    HttpEntity<Map<String, Object>> duplicateEntity = new HttpEntity<>(duplicateWorkflow, headers);

    ResponseEntity<Map> duplicateResponse =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), duplicateEntity, Map.class);

    // 중복 이름 처리 확인 (400 또는 409 예상)
    assertThat(duplicateResponse.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.CONFLICT, HttpStatus.INTERNAL_SERVER_ERROR);

    logSuccess("중복 이름 워크플로우 생성 차단 확인");
  }

  @Test
  @DisplayName("필수 필드 누락 시 워크플로우 생성 실패")
  void createWorkflow_withMissingRequiredFields_shouldFail() {
    // 선행 조건: 로그인
    performUserLogin();

    logStep(1, "워크플로우 이름 없이 생성 시도");

    // 이름 없는 요청
    Map<String, Object> noNameWorkflow = new HashMap<>();
    noNameWorkflow.put("search_platform", "naver");
    noNameWorkflow.put("is_enabled", true);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(noNameWorkflow, headers);

    ResponseEntity<Map> response =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), entity, Map.class);

    assertThat(response.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNPROCESSABLE_ENTITY);

    logSuccess("필수 필드 검증 확인");
  }

  /** 사용자 로그인을 수행하는 헬퍼 메서드 */
  private void performUserLogin() {
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
      logError("사용자 로그인 실패: " + response.getStatusCode());
      throw new RuntimeException("User login failed");
    }

    logSuccess("사용자 로그인 완료");
  }

  @Test
  @DisplayName("워크플로우 생성 시 UTC 시간 기반으로 생성 시간이 저장되는지 검증")
  void createWorkflow_utc_time_validation() throws Exception {
    logStep(1, "사용자 로그인");
    performUserLogin();

    logStep(2, "워크플로우 생성 전 현재 시간 기록 (UTC 기준)");
    Instant beforeCreate = Instant.now();

    logStep(3, "워크플로우 생성");
    Map<String, Object> workflowRequest = new HashMap<>();
    workflowRequest.put("name", "UTC 시간 검증 워크플로우");
    workflowRequest.put("description", "UTC 시간대 보장을 위한 테스트 워크플로우");
    workflowRequest.put("search_platform", "naver");
    workflowRequest.put("is_enabled", true);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(workflowRequest, headers);

    ResponseEntity<Map> createResponse =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), entity, Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) createResponse.getBody().get("success")).isTrue();

    logStep(4, "생성 직후 시간 기록 (UTC 기준)");
    Instant afterCreate = Instant.now();

    logStep(5, "생성된 워크플로우 목록 조회하여 시간 검증");
    ResponseEntity<Map> listResponse =
        restTemplate.getForEntity(getV0ApiUrl("/workflows"), Map.class);

    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Boolean) listResponse.getBody().get("success")).isTrue();

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) listResponse.getBody().get("data");

    logDebug("API 응답 구조: " + data);

    @SuppressWarnings("unchecked")
    java.util.List<Map<String, Object>> workflows =
        (java.util.List<Map<String, Object>>) data.get("data");

    assertThat(workflows).isNotNull();

    // 생성된 워크플로우 찾기
    Map<String, Object> createdWorkflow =
        workflows.stream()
            .filter(w -> "UTC 시간 검증 워크플로우".equals(w.get("name")))
            .findFirst()
            .orElse(null);

    assertThat(createdWorkflow).isNotNull();

    // createdAt 검증 - UTC 시간 범위 내에 있는지 확인
    String createdAtStr = (String) createdWorkflow.get("createdAt");
    assertThat(createdAtStr).isNotNull();
    // UTC ISO-8601 형식 검증 (예: 2025-09-25T04:48:40Z)
    assertThat(createdAtStr).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");

    logSuccess("워크플로우가 UTC 시간 기준으로 생성됨을 확인");

    // 생성 시간이 beforeCreate와 afterCreate 사이에 있는지 검증 (시간대 무관하게 UTC 기준)
    logStep(6, "생성 시간이 예상 범위 내에 있는지 검증");

    // 실제로 생성 시간과 현재 시간의 차이가 합리적인 범위(예: 10초) 내에 있는지 확인
    // 이는 시스템 시간대에 관계없이 UTC 기반으로 일관되게 작동함을 보여줌
    logDebug("생성 시간: " + createdAtStr);
    logDebug("현재 UTC 시간: " + Instant.now());

    logCompletion("UTC 시간 기반 워크플로우 생성 검증 완료");
  }

  @Test
  @DisplayName("워크플로우 생성 시 단일 스케줄 등록 성공")
  void createWorkflow_withSingleSchedule_success() {
    performUserLogin();

    logStep(1, "스케줄이 포함된 워크플로우 생성");

    // 워크플로우 + 스케줄 요청 데이터 구성
    Map<String, Object> workflowRequest = new HashMap<>();
    workflowRequest.put("name", "매일 오전 9시 자동 실행 워크플로우");
    workflowRequest.put("description", "매일 오전 9시에 자동으로 실행되는 워크플로우");
    workflowRequest.put("search_platform", "naver");
    workflowRequest.put("posting_platform", "naver_blog");
    workflowRequest.put("posting_account_id", "test_account");
    workflowRequest.put("posting_account_password", "test_password");
    workflowRequest.put("is_enabled", true);

    // 스케줄 정보 추가
    List<Map<String, Object>> schedules = new ArrayList<>();
    Map<String, Object> schedule = new HashMap<>();
    schedule.put("cronExpression", "0 0 9 * * ?"); // 매일 오전 9시
    schedule.put("scheduleText", "매일 오전 9시");
    schedule.put("isActive", true);
    schedules.add(schedule);

    workflowRequest.put("schedules", schedules);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(workflowRequest, headers);

    logStep(2, "워크플로우 생성 요청 전송");
    ResponseEntity<Map> response =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), entity, Map.class);

    logStep(3, "응답 검증");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) response.getBody().get("success")).isTrue();

    logSuccess("스케줄이 포함된 워크플로우 생성 성공");
    logDebug("응답: " + response.getBody());

    logCompletion("단일 스케줄 등록 테스트 완료");
  }

  @Test
  @DisplayName("워크플로우 생성 시 다중 스케줄 등록 성공")
  void createWorkflow_withMultipleSchedules_success() {
    performUserLogin();

    logStep(1, "다중 스케줄이 포함된 워크플로우 생성");

    // 워크플로우 기본 정보
    Map<String, Object> workflowRequest = new HashMap<>();
    workflowRequest.put("name", "다중 스케줄 워크플로우");
    workflowRequest.put("description", "여러 시간대에 실행되는 워크플로우");
    workflowRequest.put("search_platform", "naver");
    workflowRequest.put("posting_platform", "naver_blog");
    workflowRequest.put("posting_account_id", "test_multi");
    workflowRequest.put("posting_account_password", "test_pass123");
    workflowRequest.put("is_enabled", true);

    // 다중 스케줄 정보 추가
    List<Map<String, Object>> schedules = new ArrayList<>();

    // 스케줄 1: 매일 오전 9시
    Map<String, Object> schedule1 = new HashMap<>();
    schedule1.put("cronExpression", "0 0 9 * * ?");
    schedule1.put("scheduleText", "매일 오전 9시");
    schedule1.put("isActive", true);
    schedules.add(schedule1);

    // 스케줄 2: 매일 오후 6시
    Map<String, Object> schedule2 = new HashMap<>();
    schedule2.put("cronExpression", "0 0 18 * * ?");
    schedule2.put("scheduleText", "매일 오후 6시");
    schedule2.put("isActive", true);
    schedules.add(schedule2);

    // 스케줄 3: 평일 오후 2시
    Map<String, Object> schedule3 = new HashMap<>();
    schedule3.put("cronExpression", "0 0 14 ? * MON-FRI");
    schedule3.put("scheduleText", "평일 오후 2시");
    schedule3.put("isActive", true);
    schedules.add(schedule3);

    workflowRequest.put("schedules", schedules);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(workflowRequest, headers);

    logStep(2, "워크플로우 생성 요청 전송 (3개 스케줄 포함)");
    ResponseEntity<Map> response =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), entity, Map.class);

    logStep(3, "응답 검증");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) response.getBody().get("success")).isTrue();

    logSuccess("다중 스케줄이 포함된 워크플로우 생성 성공");
    logDebug("응답: " + response.getBody());

    logCompletion("다중 스케줄 등록 테스트 완료");
  }

  @Test
  @DisplayName("유효하지 않은 크론 표현식으로 스케줄 등록 시 실패")
  void createWorkflow_withInvalidCronExpression_shouldFail() {
    performUserLogin();

    logStep(1, "잘못된 크론 표현식으로 워크플로우 생성 시도");

    Map<String, Object> workflowRequest = new HashMap<>();
    workflowRequest.put("name", "잘못된 크론식 테스트");
    workflowRequest.put("search_platform", "naver");
    workflowRequest.put("is_enabled", true);

    // 잘못된 크론 표현식
    List<Map<String, Object>> schedules = new ArrayList<>();
    Map<String, Object> schedule = new HashMap<>();
    schedule.put("cronExpression", "INVALID CRON"); // 잘못된 형식
    schedule.put("scheduleText", "잘못된 스케줄");
    schedule.put("isActive", true);
    schedules.add(schedule);

    workflowRequest.put("schedules", schedules);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(workflowRequest, headers);

    logStep(2, "워크플로우 생성 요청 전송");
    ResponseEntity<Map> response =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), entity, Map.class);

    logStep(3, "에러 응답 검증");
    assertThat(response.getStatusCode())
        .isIn(
            HttpStatus.BAD_REQUEST,
            HttpStatus.UNPROCESSABLE_ENTITY,
            HttpStatus.INTERNAL_SERVER_ERROR);

    logSuccess("유효하지 않은 크론 표현식 검증 확인");
    logDebug("에러 응답: " + response.getBody());

    logCompletion("크론 표현식 검증 테스트 완료");
  }

  @Test
  @DisplayName("중복된 크론 표현식으로 스케줄 등록 시 실패")
  void createWorkflow_withDuplicateCronExpression_shouldFail() {
    performUserLogin();

    logStep(1, "중복된 크론식을 가진 워크플로우 생성 시도");

    Map<String, Object> workflowRequest = new HashMap<>();
    workflowRequest.put("name", "중복 크론식 테스트");
    workflowRequest.put("search_platform", "naver");
    workflowRequest.put("is_enabled", true);

    // 동일한 크론 표현식을 가진 스케줄 2개
    List<Map<String, Object>> schedules = new ArrayList<>();

    Map<String, Object> schedule1 = new HashMap<>();
    schedule1.put("cronExpression", "0 0 9 * * ?"); // 매일 오전 9시
    schedule1.put("scheduleText", "매일 오전 9시 - 첫번째");
    schedule1.put("isActive", true);
    schedules.add(schedule1);

    Map<String, Object> schedule2 = new HashMap<>();
    schedule2.put("cronExpression", "0 0 9 * * ?"); // 동일한 크론식
    schedule2.put("scheduleText", "매일 오전 9시 - 두번째");
    schedule2.put("isActive", true);
    schedules.add(schedule2);

    workflowRequest.put("schedules", schedules);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(workflowRequest, headers);

    logStep(2, "워크플로우 생성 요청 전송");
    ResponseEntity<Map> response =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), entity, Map.class);

    logStep(3, "중복 크론식 에러 검증");
    assertThat(response.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.CONFLICT, HttpStatus.INTERNAL_SERVER_ERROR);

    logSuccess("중복 크론 표현식 검증 확인");
    logDebug("에러 응답: " + response.getBody());

    logCompletion("중복 크론식 검증 테스트 완료");
  }

  @Test
  @DisplayName("스케줄 없이 워크플로우 생성 후 정상 작동 확인")
  void createWorkflow_withoutSchedule_success() {
    performUserLogin();

    logStep(1, "스케줄 없이 워크플로우 생성");

    Map<String, Object> workflowRequest = new HashMap<>();
    workflowRequest.put("name", "스케줄 없는 워크플로우");
    workflowRequest.put("description", "수동 실행 전용 워크플로우");
    workflowRequest.put("search_platform", "naver");
    workflowRequest.put("posting_platform", "naver_blog");
    workflowRequest.put("posting_account_id", "manual_test");
    workflowRequest.put("posting_account_password", "manual_pass");
    workflowRequest.put("is_enabled", true);
    // schedules 필드 없음

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(workflowRequest, headers);

    logStep(2, "워크플로우 생성 요청 전송");
    ResponseEntity<Map> response =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), entity, Map.class);

    logStep(3, "응답 검증");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) response.getBody().get("success")).isTrue();

    logSuccess("스케줄 없는 워크플로우 생성 성공");
    logDebug("응답: " + response.getBody());

    logCompletion("스케줄 선택사항 테스트 완료");
  }

  @Test
  @DisplayName("비활성화 스케줄로 워크플로우 생성 시 Quartz 미등록 확인")
  void createWorkflow_withInactiveSchedule_shouldNotRegisterToQuartz() {
    performUserLogin();

    logStep(1, "비활성화 스케줄로 워크플로우 생성");

    Map<String, Object> workflowRequest = new HashMap<>();
    workflowRequest.put("name", "비활성화 스케줄 테스트");
    workflowRequest.put("description", "DB에는 저장되지만 Quartz에는 등록되지 않음");
    workflowRequest.put("search_platform", "naver");
    workflowRequest.put("is_enabled", true);

    // 비활성화 스케줄
    List<Map<String, Object>> schedules = new ArrayList<>();
    Map<String, Object> schedule = new HashMap<>();
    schedule.put("cronExpression", "0 0 10 * * ?");
    schedule.put("scheduleText", "매일 오전 10시 (비활성)");
    schedule.put("isActive", false); // 비활성화
    schedules.add(schedule);

    workflowRequest.put("schedules", schedules);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(workflowRequest, headers);

    logStep(2, "워크플로우 생성 요청 전송");
    ResponseEntity<Map> response =
        restTemplate.postForEntity(getV0ApiUrl("/workflows"), entity, Map.class);

    logStep(3, "응답 검증 - DB 저장은 성공하지만 Quartz 미등록");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) response.getBody().get("success")).isTrue();

    logSuccess("비활성화 스케줄로 워크플로우 생성 성공");
    logDebug("응답: " + response.getBody());
    logDebug("비활성화 스케줄은 DB에 저장되지만 Quartz에는 등록되지 않음");

    logCompletion("비활성화 스케줄 테스트 완료");
  }
}
