package site.icebang.domain.workflow.service;

import java.math.BigInteger;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import site.icebang.common.dto.PageParams;
import site.icebang.common.dto.PageResult;
import site.icebang.common.exception.DuplicateDataException;
import site.icebang.common.service.PageableService;
import site.icebang.domain.schedule.mapper.ScheduleMapper;
import site.icebang.domain.schedule.model.Schedule;
import site.icebang.domain.schedule.service.QuartzScheduleService;
import site.icebang.domain.workflow.dto.*;
import site.icebang.domain.workflow.mapper.JobMapper;
import site.icebang.domain.workflow.mapper.TaskIoDataMapper;
import site.icebang.domain.workflow.mapper.TaskMapper;
import site.icebang.domain.workflow.mapper.WorkflowMapper;
import site.icebang.domain.workflow.model.TaskIoData;

/**
 * 워크플로우의 '정의'와 관련된 비즈니스 로직을 처리하는 서비스 클래스입니다.
 *
 * <p>이 서비스는 워크플로우의 실행(Execution)이 아닌, 생성된 워크플로우의 구조를 조회하는 기능에 집중합니다.
 *
 * <h2>주요 기능:</h2>
 *
 * <ul>
 *   <li>워크플로우 목록 페이징 조회
 *   <li>특정 워크플로우의 상세 구조 조회 (Job, Task, Schedule 포함)
 * </ul>
 *
 * @author jihu0210@naver.com
 * @since v0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService implements PageableService<WorkflowCardDto> {

  private final WorkflowMapper workflowMapper;
  private final ScheduleMapper scheduleMapper;
  private final QuartzScheduleService quartzScheduleService;
  private final JobMapper jobMapper;
  private final TaskMapper taskMapper;
  private final TaskIoDataMapper taskIoDataMapper;

  /**
   * 워크플로우 목록을 페이징 처리하여 조회합니다.
   *
   * <p>이 메소드는 {@code PageableService} 인터페이스를 구현하며, {@code PageResult} 유틸리티를 사용하여 전체 카운트 쿼리와 목록 조회
   * 쿼리를 실행하고 페이징 결과를 생성합니다.
   *
   * @param pageParams 페이징 처리에 필요한 파라미터 (페이지 번호, 페이지 크기 등)
   * @return 페이징 처리된 워크플로우 카드 목록
   * @see PageResult
   * @since v0.1.0
   */
  @Override
  @Transactional(readOnly = true)
  public PageResult<WorkflowCardDto> getPagedResult(PageParams pageParams) {
    return PageResult.from(
        pageParams,
        () -> workflowMapper.selectWorkflowList(pageParams),
        () -> workflowMapper.selectWorkflowCount(pageParams));
  }

  /**
   * 특정 워크플로우의 상세 구조를 조회합니다.
   *
   * <p>지정된 워크플로우 ID에 해당하는 기본 정보, 연결된 스케줄 목록, 그리고 Job과 Task의 계층 구조를 모두 조회하여 하나의 DTO로 조합하여 반환합니다.
   *
   * @param workflowId 조회할 워크플로우의 ID
   * @return 워크플로우의 전체 구조를 담은 상세 DTO
   * @throws IllegalArgumentException 주어진 ID에 해당하는 워크플로우가 존재하지 않을 경우
   * @since v0.1.0
   */
  @Transactional(readOnly = true)
  public WorkflowDetailCardDto getWorkflowDetail(BigInteger workflowId) {

    // 1. 워크플로우 기본 정보 조회 (단일 row, 효율적)
    WorkflowDetailCardDto workflow = workflowMapper.selectWorkflowDetailById(workflowId);
    if (workflow == null) {
      throw new IllegalArgumentException("워크플로우를 찾을 수 없습니다: " + workflowId);
    }

    // 2. 스케줄 목록 조회 (별도 쿼리로 성능 최적화)
    List<ScheduleDto> schedules = workflowMapper.selectSchedulesByWorkflowId(workflowId);
    workflow.setSchedules(schedules);

    List<Map<String, Object>> jobs = workflowMapper.selectWorkflowWithJobsAndTasks(workflowId);
    workflow.setJobs(jobs);

    return workflow;
  }

  /**
   * 워크플로우 생성 (스케줄 포함 가능)
   *
   * <p>워크플로우와 스케줄을 하나의 트랜잭션으로 처리하여 원자성을 보장합니다. 스케줄이 포함된 경우 DB 저장 후 즉시 Quartz 스케줄러에 등록합니다.
   *
   * @param dto 워크플로우 생성 정보 (스케줄 선택사항)
   * @param createdBy 생성자 ID
   * @throws IllegalArgumentException 검증 실패 시
   * @throws RuntimeException 생성 중 오류 발생 시
   */
  @Transactional
  public void createWorkflow(WorkflowCreateDto dto, BigInteger createdBy) {
    // 1. 기본 검증
    validateBasicInput(dto, createdBy);

    // 2. 비즈니스 검증
    validateBusinessRules(dto);

    // 3. 스케줄 검증 (있는 경우만)
    if (dto.hasSchedules()) {
      validateSchedules(dto.getSchedules());
    }

    // 4. 워크플로우 이름 중복 체크
    if (workflowMapper.existsByName(dto.getName())) {
      throw new IllegalArgumentException("이미 존재하는 워크플로우 이름입니다: " + dto.getName());
    }

    // 5. 워크플로우 생성
    Long workflowId = null;
    try {
      // JSON 설정 생성
      String defaultConfigJson = dto.genertateDefaultConfigJson();
      dto.setDefaultConfigJson(defaultConfigJson);

      // DB 삽입 파라미터 구성
      Map<String, Object> params = new HashMap<>();
      params.put("dto", dto);
      params.put("createdBy", createdBy);

      int result = workflowMapper.insertWorkflow(params);
      if (result != 1) {
        throw new RuntimeException("워크플로우 생성에 실패했습니다");
      }

      // 생성된 workflow ID 추출
      Object generatedId = params.get("id");
      workflowId =
          (generatedId instanceof BigInteger)
              ? ((BigInteger) generatedId).longValue()
              : ((Number) generatedId).longValue();

      log.info("워크플로우 생성 완료: {} (ID: {}, 생성자: {})", dto.getName(), workflowId, createdBy);

    } catch (Exception e) {
      log.error("워크플로우 생성 실패: {}", dto.getName(), e);
      throw new RuntimeException("워크플로우 생성 중 오류가 발생했습니다", e);
    }

    // 6. 스케줄 등록 (있는 경우만)
    if (dto.hasSchedules() && workflowId != null) {
      registerSchedules(workflowId, dto.getSchedules(), createdBy.longValue());
    }
  }

  /**
   * Job 생성
   *
   * @param dto Job 생성 정보
   * @return 생성된 Job 정보
   * @throws IllegalArgumentException Job 이름이 필수인데 없거나 빈 값일 경우
   */
  @Transactional
  public JobDto createJob(JobDto dto) {
    // 1. 유효성 검증
    if (dto.getName() == null || dto.getName().isBlank()) {
      throw new IllegalArgumentException("job name is required");
    }

    // 2. 시간 정보 설정
    Instant now = Instant.now();
    dto.setCreatedAt(now);
    dto.setUpdatedAt(now);

    // 3. 생성자 정보 설정 (현재 사용자 정보가 없으므로 기본값 또는 추후 개선)
    // dto.setCreatedBy(getCurrentUserId());
    // dto.setUpdatedBy(getCurrentUserId());

    // 4. DB 저장
    jobMapper.insertJob(dto);

    // 5. 저장된 Job 반환
    return jobMapper.findJobById(dto.getId());
  }

  /**
   * Job ID로 Job 조회
   *
   * @param id Job ID
   * @return Job 정보, 없으면 null
   */
  @Transactional(readOnly = true)
  public JobDto findJobById(Long id) {
    return jobMapper.findJobById(id);
  }

  /**
   * Task 생성
   *
   * @param dto Task 생성 정보
   * @return 생성된 Task 정보
   * @throws IllegalArgumentException Task 이름이 필수인데 없거나 빈 값일 경우
   */
  @Transactional
  public TaskDto createTask(TaskDto dto) {
    // 1. 유효성 검증
    if (dto.getName() == null || dto.getName().isBlank()) {
      throw new IllegalArgumentException("task name is required");
    }
    // 2. 시간 정보 설정
    Instant now = Instant.now();
    dto.setCreatedAt(now);
    dto.setUpdatedAt(now);

    // 3. 생성자 정보 설정 (현재 사용자 정보가 없으므로 기본값 또는 추후 개선)
    // dto.setCreatedBy(getCurrentUserId());
    // dto.setUpdatedBy(getCurrentUserId());

    // 4. DB 저장
    taskMapper.insertTask(dto);

    // 5. 저장된 Task 반환
    return taskMapper.findTaskById(dto.getId());
  }

  /**
   * Task ID로 Task 조회
   *
   * @param id Task ID
   * @return Task 정보, 없으면 null
   */
  @Transactional(readOnly = true)
  public TaskDto findTaskById(Long id) {
    return taskMapper.findTaskById(id);
  }

  /**
   * Task Run ID 목록으로 Task IO 데이터 조회
   *
   * @param taskRunIds Task Run ID 목록
   * @param ioType IO 타입 필터 ("INPUT", "OUTPUT", null이면 모두 조회)
   * @param limit 조회 제한 수 (null이면 모두 조회)
   * @return Task IO 데이터 목록 (created_at 기준 내림차순 정렬)
   */
  @Transactional(readOnly = true)
  public List<TaskIoData> getTaskIoDataByTaskRunIds(
      List<Long> taskRunIds, String ioType, Integer limit) {
    if (taskRunIds == null || taskRunIds.isEmpty()) {
      return List.of();
    }
    return taskIoDataMapper.findByTaskRunIds(taskRunIds, ioType, limit);
  }

  /** 기본 입력값 검증 */
  private void validateBasicInput(WorkflowCreateDto dto, BigInteger createdBy) {
    if (dto == null) {
      throw new IllegalArgumentException("워크플로우 정보가 필요합니다");
    }
    if (createdBy == null) {
      throw new IllegalArgumentException("생성자 정보가 필요합니다");
    }
  }

  /** 비즈니스 규칙 검증 */
  private void validateBusinessRules(WorkflowCreateDto dto) {
    // 포스팅 플랫폼 선택 시 계정 정보 필수 검증
    String postingPlatform = dto.getPostingPlatform();
    if (postingPlatform != null && !postingPlatform.isBlank()) {
      if (dto.getPostingAccountId() == null || dto.getPostingAccountId().isBlank()) {
        throw new IllegalArgumentException("포스팅 플랫폼 선택 시 계정 ID는 필수입니다");
      }
      if (dto.getPostingAccountPassword() == null || dto.getPostingAccountPassword().isBlank()) {
        throw new IllegalArgumentException("포스팅 플랫폼 선택 시 계정 비밀번호는 필수입니다");
      }
      // 티스토리 블로그 추가 검증
      if ("tstory_blog".equals(postingPlatform)) {
        if (dto.getBlogName() == null || dto.getBlogName().isBlank()) {
          throw new IllegalArgumentException("티스토리 블로그 선택 시 블로그 이름은 필수입니다");
        }
      }
    }
  }

  /**
   * 스케줄 목록 검증
   *
   * <p>크론 표현식 유효성 및 중복 검사를 수행합니다.
   *
   * @param schedules 검증할 스케줄 목록
   * @throws IllegalArgumentException 유효하지 않은 크론식
   * @throws DuplicateDataException 중복 크론식 발견
   */
  private void validateSchedules(List<ScheduleCreateDto> schedules) {
    if (schedules == null || schedules.isEmpty()) {
      return;
    }

    // 중복 크론식 검사 (같은 요청 내에서)
    Set<String> cronExpressions = new HashSet<>();

    for (ScheduleCreateDto schedule : schedules) {
      String cron = schedule.getCronExpression();

      // 1. 크론 표현식 유효성 검증 (Quartz 기준)
      if (!isValidCronExpression(cron)) {
        throw new IllegalArgumentException("유효하지 않은 크론 표현식입니다: " + cron);
      }

      // 2. 중복 크론식 검사
      if (cronExpressions.contains(cron)) {
        throw new DuplicateDataException("중복된 크론 표현식이 있습니다: " + cron);
      }
      cronExpressions.add(cron);
    }
  }

  /**
   * Quartz 크론 표현식 유효성 검증
   *
   * @param cronExpression 검증할 크론 표현식
   * @return 유효하면 true
   */
  private boolean isValidCronExpression(String cronExpression) {
    try {
      new CronExpression(cronExpression);
      return true;
    } catch (Exception e) {
      log.warn("유효하지 않은 크론 표현식: {}", cronExpression, e);
      return false;
    }
  }

  /**
   * 스케줄 목록 등록 (DB 저장 + Quartz 등록)
   *
   * <p>트랜잭션 내에서 DB 저장을 수행하고, Quartz 등록은 실패해도 워크플로우는 유지되도록 예외를 로그로만 처리합니다.
   *
   * @param workflowId 워크플로우 ID
   * @param scheduleCreateDtos 등록할 스케줄 목록
   * @param userId 생성자 ID
   */
  private void registerSchedules(
      Long workflowId, List<ScheduleCreateDto> scheduleCreateDtos, Long userId) {
    if (scheduleCreateDtos == null || scheduleCreateDtos.isEmpty()) {
      return;
    }

    log.info("스케줄 등록 시작: Workflow ID {} - {}개", workflowId, scheduleCreateDtos.size());

    int successCount = 0;
    int failCount = 0;

    for (ScheduleCreateDto dto : scheduleCreateDtos) {
      try {
        // 1. DTO → Model 변환
        Schedule schedule = dto.toEntity(workflowId, userId);

        // 2. DB 중복 체크 (같은 워크플로우 + 같은 크론식)
        if (scheduleMapper.existsByWorkflowIdAndCronExpression(
            workflowId, schedule.getCronExpression())) {
          throw new DuplicateDataException(
              "이미 동일한 크론식의 스케줄이 존재합니다: " + schedule.getCronExpression());
        }

        // 3. DB 저장
        int insertResult = scheduleMapper.insertSchedule(schedule);
        if (insertResult != 1) {
          log.error("스케줄 DB 저장 실패: Workflow ID {} - {}", workflowId, schedule.getCronExpression());
          failCount++;
          continue;
        }

        // 4. Quartz 등록 (실시간 반영)
        quartzScheduleService.addOrUpdateSchedule(schedule);

        log.info(
            "스케줄 등록 완료: Workflow ID {} - {} ({})",
            workflowId,
            schedule.getCronExpression(),
            schedule.getScheduleText());
        successCount++;

      } catch (DuplicateDataException e) {
        log.warn("스케줄 중복으로 등록 건너뜀: Workflow ID {} - {}", workflowId, dto.getCronExpression());
        failCount++;
        // 중복은 경고만 하고 계속 진행
      } catch (Exception e) {
        log.error("스케줄 등록 실패: Workflow ID {} - {}", workflowId, dto.getCronExpression(), e);
        failCount++;
        // 스케줄 등록 실패해도 워크플로우는 유지
      }
    }

    log.info("스케줄 등록 완료: Workflow ID {} - 성공 {}개, 실패 {}개", workflowId, successCount, failCount);
  }
}
