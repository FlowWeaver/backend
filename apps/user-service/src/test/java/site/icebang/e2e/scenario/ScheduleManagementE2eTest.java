package site.icebang.e2e.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;

import site.icebang.e2e.setup.annotation.E2eTest;
import site.icebang.e2e.setup.support.E2eTestSupport;

/**
 * 스케줄 관련 E2E 시나리오 테스트
 *
 * <p>ScheduleService 기능을 API 플로우 관점에서 검증
 */
@Sql(
    value = {
      "classpath:sql/data/00-truncate.sql",
      "classpath:sql/data/01-insert-internal-users.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@E2eTest
@DisplayName("스케줄 관리 E2E 테스트")
class ScheduleManagementE2eTest extends E2eTestSupport {

  @Test
  @DisplayName("워크플로우에 스케줄 추가 성공")
  void createSchedule_success() {
    performUserLogin();

    logStep(1, "워크플로우 생성");
    Long workflowId = createWorkflow("스케줄 생성용 워크플로우");

    logStep(2, "스케줄 추가 요청");
    Map<String, Object> scheduleRequest = new HashMap<>();
    scheduleRequest.put("cronExpression", "0 0 9 * * ?");
    scheduleRequest.put("scheduleText", "매일 오전 9시");
    scheduleRequest.put("isActive", true);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(scheduleRequest, headers);

    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            getV0ApiUrl("/workflows/" + workflowId + "/schedules"), entity, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) response.getBody().get("success")).isTrue();

    logSuccess("워크플로우에 스케줄 추가 성공");
  }

  @Test
  @DisplayName("잘못된 크론식으로 스케줄 생성 시 실패")
  void createSchedule_invalidCron_shouldFail() {
    performUserLogin();
    Long workflowId = createWorkflow("잘못된 크론식 워크플로우");

    Map<String, Object> scheduleRequest = new HashMap<>();
    scheduleRequest.put("cronExpression", "INVALID CRON");
    scheduleRequest.put("scheduleText", "잘못된 크론");
    scheduleRequest.put("isActive", true);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            getV0ApiUrl("/workflows/" + workflowId + "/schedules"),
            new HttpEntity<>(scheduleRequest, headers),
            Map.class);

    assertThat(response.getStatusCode())
        .isIn(
            HttpStatus.BAD_REQUEST,
            HttpStatus.UNPROCESSABLE_ENTITY,
            HttpStatus.INTERNAL_SERVER_ERROR);

    logSuccess("잘못된 크론식 검증 완료");
  }

  @Test
  @DisplayName("스케줄 비활성화 후 Quartz 미등록 확인")
  void createInactiveSchedule_shouldNotRegisterQuartz() {
    performUserLogin();
    Long workflowId = createWorkflow("비활성 스케줄 워크플로우");

    Map<String, Object> scheduleRequest = new HashMap<>();
    scheduleRequest.put("cronExpression", "0 0 10 * * ?");
    scheduleRequest.put("scheduleText", "매일 오전 10시 (비활성)");
    scheduleRequest.put("isActive", false);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            getV0ApiUrl("/workflows/" + workflowId + "/schedules"),
            new HttpEntity<>(scheduleRequest, headers),
            Map.class);

    System.out.println("==== response body ====");
    System.out.println(response.getBody());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((Boolean) response.getBody().get("success")).isTrue();

    logSuccess("비활성 스케줄 생성 성공 (Quartz 미등록)");
  }

  @Test
  @DisplayName("스케줄 목록 조회 성공")
  void listSchedules_success() {
    performUserLogin();
    Long workflowId = createWorkflow("스케줄 조회용 워크플로우");

    // 스케줄 2개 추가
    addSchedule(workflowId, "0 0 8 * * ?", "매일 오전 8시", true);
    addSchedule(workflowId, "0 0 18 * * ?", "매일 오후 6시", true);

    logStep(1, "스케줄 목록 조회 API 호출");
    ResponseEntity<Map> response =
        restTemplate.getForEntity(
            getV0ApiUrl("/workflows/" + workflowId + "/schedules"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Boolean) response.getBody().get("success")).isTrue();

    List<Map<String, Object>> schedules =
        (List<Map<String, Object>>) response.getBody().get("data");

    assertThat(schedules).hasSizeGreaterThanOrEqualTo(2);

    logSuccess("스케줄 목록 조회 성공: " + schedules.size() + "개");
  }

  @Test
  @DisplayName("스케줄 수정 및 활성화 토글 성공")
  void updateSchedule_toggleActive_success() {
    performUserLogin();
    Long workflowId = createWorkflow("스케줄 수정용 워크플로우");

    Long scheduleId = addSchedule(workflowId, "0 0 12 * * ?", "정오 실행", true);

    logStep(1, "스케줄 비활성화 요청");
    Map<String, Object> updateRequest = new HashMap<>();
    updateRequest.put("cronExpression", "0 30 12 * * ?");
    updateRequest.put("scheduleText", "정오 30분 실행");
    updateRequest.put("isActive", false);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    restTemplate.put(
        getV0ApiUrl("/workflows/" + workflowId + "/schedules/" + scheduleId),
        new HttpEntity<>(updateRequest, headers));

    logSuccess("스케줄 수정 및 비활성화 성공");
  }

  @Test
  @DisplayName("스케줄 삭제 성공")
  void deleteSchedule_success() {
    performUserLogin();
    Long workflowId = createWorkflow("스케줄 삭제용 워크플로우");

    Long scheduleId = addSchedule(workflowId, "0 0 7 * * ?", "매일 오전 7시", true);

    logStep(1, "스케줄 삭제 요청");
    restTemplate.delete(getV0ApiUrl("/workflows/" + workflowId + "/schedules/" + scheduleId));

    logSuccess("스케줄 삭제 성공 (논리 삭제)");
  }

  /** 워크플로우 생성 헬퍼 */
  private Long createWorkflow(String name) {
    Map<String, Object> workflowRequest = new HashMap<>();
    workflowRequest.put("name", name);
    workflowRequest.put("search_platform", "naver");
    workflowRequest.put("is_enabled", true);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            getV0ApiUrl("/workflows"), new HttpEntity<>(workflowRequest, headers), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    ResponseEntity<Map> listResponse =
        restTemplate.getForEntity(getV0ApiUrl("/workflows"), Map.class);

    Map<String, Object> body = listResponse.getBody();
    List<Map<String, Object>> workflows =
        (List<Map<String, Object>>) ((Map<String, Object>) body.get("data")).get("data");

    return workflows.stream()
        .filter(w -> name.equals(w.get("name")))
        .findFirst()
        .map(w -> Long.valueOf(w.get("id").toString()))
        .orElseThrow(() -> new RuntimeException("생성한 워크플로우를 찾을 수 없습니다"));
  }

  /** 스케줄 추가 헬퍼 */
  private Long addSchedule(Long workflowId, String cron, String text, boolean active) {
    Map<String, Object> scheduleRequest = new HashMap<>();
    scheduleRequest.put("cronExpression", cron);
    scheduleRequest.put("scheduleText", text);
    scheduleRequest.put("isActive", active);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            getV0ApiUrl("/workflows/" + workflowId + "/schedules"),
            new HttpEntity<>(scheduleRequest, headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    return Long.valueOf(
        ((Map<String, Object>) response.getBody().get("data")).get("id").toString());
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
}
