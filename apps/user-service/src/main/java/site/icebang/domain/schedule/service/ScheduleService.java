package site.icebang.domain.schedule.service;

import java.util.List;

import org.quartz.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import site.icebang.domain.schedule.mapper.ScheduleMapper;
import site.icebang.domain.schedule.model.Schedule;
import site.icebang.domain.workflow.dto.ScheduleCreateDto;
import site.icebang.domain.workflow.dto.ScheduleUpdateDto;

/**
 * 스케줄 관리를 위한 비즈니스 로직을 처리하는 서비스 클래스입니다.
 *
 * <p>이 서비스는 스케줄의 CRUD 작업과 Quartz 스케줄러와의 동기화를 담당합니다.
 *
 * <h2>주요 기능:</h2>
 *
 * <ul>
 *   <li>스케줄 조회 (단건, 목록)
 *   <li>스케줄 수정 (크론식, 활성화 상태)
 *   <li>스케줄 삭제 (논리 삭제)
 *   <li>스케줄 활성화/비활성화 토글
 *   <li>DB 변경 시 Quartz 실시간 동기화
 * </ul>
 *
 * @author bwnfo0702@gmail.com
 * @since v0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

  private final ScheduleMapper scheduleMapper;
  private final QuartzScheduleService quartzScheduleService;

  @Transactional
  public Schedule createSchedule(Long workflowId, ScheduleCreateDto dto, Long userId) {
    // 1. Schedule 엔티티 생성
    Schedule schedule =
        Schedule.builder()
            .workflowId(workflowId)
            .cronExpression(dto.getCronExpression())
            .scheduleText(dto.getScheduleText())
            .isActive(dto.getIsActive())
            .createdBy(userId)
            .build();

    // 2. DB에 저장
    scheduleMapper.insertSchedule(schedule);

    // 3. 활성화 상태면 Quartz에 등록
    if (schedule.isActive()) {
      quartzScheduleService.addOrUpdateSchedule(schedule);
    }

    return schedule;
  }

  /**
   * 특정 워크플로우의 모든 활성 스케줄을 조회합니다.
   *
   * @param workflowId 워크플로우 ID
   * @return 활성 스케줄 목록
   */
  @Transactional(readOnly = true)
  public List<Schedule> getSchedulesByWorkflowId(Long workflowId) {
    log.debug("워크플로우 스케줄 조회: Workflow ID {}", workflowId);
    return scheduleMapper.findAllByWorkflowId(workflowId);
  }

  /**
   * 스케줄 ID로 단건 조회합니다.
   *
   * @param scheduleId 스케줄 ID
   * @return 스케줄 정보
   * @throws IllegalArgumentException 스케줄이 존재하지 않을 경우
   */
  @Transactional(readOnly = true)
  public Schedule getScheduleById(Long scheduleId) {
    Schedule schedule = scheduleMapper.findById(scheduleId);
    if (schedule == null) {
      throw new IllegalArgumentException("스케줄을 찾을 수 없습니다: " + scheduleId);
    }
    return schedule;
  }

  /**
   * 스케줄을 수정하고 Quartz에 실시간 반영합니다.
   *
   * <p>수정 프로세스:
   *
   * <ol>
   *   <li>크론 표현식 유효성 검증
   *   <li>DB 업데이트
   *   <li>Quartz 스케줄러에 변경사항 반영 (재등록)
   *   <li>비활성화된 경우 Quartz에서 제거
   * </ol>
   *
   * @param scheduleId 수정할 스케줄 ID
   * @param dto 수정 정보
   * @param updatedBy 수정자 ID
   * @throws IllegalArgumentException 스케줄이 존재하지 않거나 크론식이 유효하지 않을 경우
   */
  @Transactional
  public void updateSchedule(Long scheduleId, ScheduleUpdateDto dto, Long updatedBy) {
    log.info("스케줄 수정 시작: Schedule ID {}", scheduleId);

    // 1. 기존 스케줄 조회
    Schedule schedule = getScheduleById(scheduleId);

    // 2. 크론 표현식 유효성 검증
    if (!isValidCronExpression(dto.getCronExpression())) {
      throw new IllegalArgumentException("유효하지 않은 크론 표현식입니다: " + dto.getCronExpression());
    }

    // 3. 스케줄 정보 업데이트
    schedule.setCronExpression(dto.getCronExpression());
    schedule.setScheduleText(dto.getScheduleText());
    schedule.setActive(dto.getIsActive());
    schedule.setUpdatedBy(updatedBy);

    // 4. DB 업데이트
    int result = scheduleMapper.updateSchedule(schedule);
    if (result != 1) {
      throw new RuntimeException("스케줄 수정에 실패했습니다: Schedule ID " + scheduleId);
    }

    // 5. Quartz 실시간 동기화
    syncScheduleToQuartz(schedule);

    log.info(
        "스케줄 수정 완료: Schedule ID {} - {} (활성화: {})",
        scheduleId,
        dto.getCronExpression(),
        dto.getIsActive());
  }

  /**
   * 스케줄 활성화 상태를 토글합니다.
   *
   * <p>활성화 → 비활성화 또는 비활성화 → 활성화로 전환하고 Quartz에 반영합니다.
   *
   * @param scheduleId 스케줄 ID
   * @param isActive 변경할 활성화 상태
   * @throws IllegalArgumentException 스케줄이 존재하지 않을 경우
   */
  @Transactional
  public void toggleScheduleActive(Long scheduleId, Boolean isActive) {
    log.info("스케줄 활성화 상태 변경: Schedule ID {} - {}", scheduleId, isActive);

    // 1. 기존 스케줄 조회
    Schedule schedule = getScheduleById(scheduleId);

    // 2. DB 업데이트
    int result = scheduleMapper.updateActiveStatus(scheduleId, isActive);
    if (result != 1) {
      throw new RuntimeException("스케줄 활성화 상태 변경 실패: Schedule ID " + scheduleId);
    }

    // 3. 스케줄 객체 상태 업데이트
    schedule.setActive(isActive);

    // 4. Quartz 실시간 동기화
    syncScheduleToQuartz(schedule);

    log.info("스케줄 활성화 상태 변경 완료: Schedule ID {} - {}", scheduleId, isActive);
  }

  /**
   * 스케줄을 삭제합니다 (논리 삭제).
   *
   * <p>DB에서 is_active를 false로 설정하고 Quartz에서도 제거합니다.
   *
   * @param scheduleId 삭제할 스케줄 ID
   * @throws IllegalArgumentException 스케줄이 존재하지 않을 경우
   */
  @Transactional
  public void deleteSchedule(Long scheduleId) {
    log.info("스케줄 삭제 시작: Schedule ID {}", scheduleId);

    // 1. 기존 스케줄 조회
    Schedule schedule = getScheduleById(scheduleId);

    // 2. DB에서 논리 삭제
    int result = scheduleMapper.deleteSchedule(scheduleId);
    if (result != 1) {
      throw new RuntimeException("스케줄 삭제에 실패했습니다: Schedule ID " + scheduleId);
    }

    // 3. Quartz에서 제거
    quartzScheduleService.deleteSchedule(schedule.getWorkflowId());

    log.info("스케줄 삭제 완료: Schedule ID {}", scheduleId);
  }

  /**
   * 스케줄 변경사항을 Quartz 스케줄러에 동기화합니다.
   *
   * <p>활성화된 스케줄: Quartz에 등록/업데이트 비활성화된 스케줄: Quartz에서 제거
   *
   * @param schedule 동기화할 스케줄
   */
  private void syncScheduleToQuartz(Schedule schedule) {
    if (schedule.isActive()) {
      // 활성화: Quartz에 등록 또는 업데이트
      quartzScheduleService.addOrUpdateSchedule(schedule);
      log.debug("Quartz 스케줄 등록/업데이트: Workflow ID {}", schedule.getWorkflowId());
    } else {
      // 비활성화: Quartz에서 제거
      quartzScheduleService.deleteSchedule(schedule.getWorkflowId());
      log.debug("Quartz 스케줄 제거: Workflow ID {}", schedule.getWorkflowId());
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
}
