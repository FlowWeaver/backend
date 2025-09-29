package site.icebang.domain.workflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import site.icebang.common.dto.ApiResponse;
import site.icebang.domain.auth.model.AuthCredential;
import site.icebang.domain.schedule.model.Schedule;
import site.icebang.domain.schedule.service.ScheduleService;
import site.icebang.domain.workflow.dto.ScheduleCreateDto;
import site.icebang.domain.workflow.dto.ScheduleUpdateDto;

/**
 * 스케줄 관리를 위한 REST API 컨트롤러입니다.
 *
 * <p>스케줄의 조회, 수정, 삭제, 활성화/비활성화 API를 제공합니다.
 *
 * <h2>제공 API:</h2>
 * <ul>
 *   <li>GET /v0/workflows/{workflowId}/schedules - 워크플로우의 스케줄 목록 조회
 *   <li>GET /v0/schedules/{scheduleId} - 스케줄 단건 조회
 *   <li>PUT /v0/schedules/{scheduleId} - 스케줄 수정
 *   <li>PATCH /v0/schedules/{scheduleId}/active - 스케줄 활성화/비활성화
 *   <li>DELETE /v0/schedules/{scheduleId} - 스케줄 삭제
 * </ul>
 *
 * @author bwnfo0702@gmail.com
 * @since v0.1.0
 */
@Slf4j
@RestController
@RequestMapping("/v0")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/workflows/{workflowId}/schedules")
    public ApiResponse<Schedule> createSchedule(
            @PathVariable Long workflowId,
            @Valid @RequestBody ScheduleCreateDto dto,
            @AuthenticationPrincipal AuthCredential authCredential) {

        Long userId = authCredential.getId().longValue();
        Schedule schedule = scheduleService.createSchedule(workflowId, dto, userId);

        return ApiResponse.success(schedule);
    }

    /**
     * 특정 워크플로우의 모든 스케줄을 조회합니다.
     *
     * @param workflowId 워크플로우 ID
     * @return 스케줄 목록
     */
    @GetMapping("/workflows/{workflowId}/schedules")
    public ApiResponse<List<Schedule>> getSchedulesByWorkflow(@PathVariable Long workflowId) {
        log.info("워크플로우 스케줄 목록 조회 요청: Workflow ID {}", workflowId);
        List<Schedule> schedules = scheduleService.getSchedulesByWorkflowId(workflowId);
        return ApiResponse.success(schedules);
    }

    /**
     * 스케줄 ID로 단건 조회합니다.
     *
     * @param scheduleId 스케줄 ID
     * @return 스케줄 정보
     */
    @GetMapping("/schedules/{scheduleId}")
    public ApiResponse<Schedule> getSchedule(@PathVariable Long scheduleId) {
        log.info("스케줄 조회 요청: Schedule ID {}", scheduleId);
        Schedule schedule = scheduleService.getScheduleById(scheduleId);
        return ApiResponse.success(schedule);
    }

    /**
     * 스케줄을 수정합니다.
     *
     * <p>크론 표현식, 스케줄 텍스트, 활성화 상태를 수정할 수 있으며, 변경사항은 즉시 Quartz에 반영됩니다.
     *
     * @param scheduleId 수정할 스케줄 ID
     * @param dto 수정 정보
     * @param authCredential 인증 정보 (수정자)
     * @return 성공 응답
     */
    @PutMapping("/schedules/{scheduleId}")
    public ApiResponse<Void> updateSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleUpdateDto dto,
            @AuthenticationPrincipal AuthCredential authCredential) {

        log.info("스케줄 수정 요청: Schedule ID {} - {}", scheduleId, dto.getCronExpression());

        // 인증 체크
        if (authCredential == null) {
            throw new IllegalArgumentException("로그인이 필요합니다");
        }

        Long userId = authCredential.getId().longValue();
        scheduleService.updateSchedule(scheduleId, dto, userId);

        return ApiResponse.success(null);
    }

    /**
     * 스케줄 활성화 상태를 변경합니다.
     *
     * <p>활성화(true) 시 Quartz에 등록되어 실행되고, 비활성화(false) 시 Quartz에서 제거됩니다.
     *
     * @param scheduleId 스케줄 ID
     * @param isActive 변경할 활성화 상태
     * @return 성공 응답
     */
    @PatchMapping("/schedules/{scheduleId}/active")
    public ApiResponse<Void> toggleScheduleActive(
            @PathVariable Long scheduleId, @RequestParam Boolean isActive) {

        log.info("스케줄 활성화 상태 변경 요청: Schedule ID {} - {}", scheduleId, isActive);
        scheduleService.toggleScheduleActive(scheduleId, isActive);

        return ApiResponse.success(null);
    }

    /**
     * 스케줄을 삭제합니다 (논리 삭제).
     *
     * <p>DB에서 비활성화되고 Quartz에서도 제거됩니다.
     *
     * @param scheduleId 삭제할 스케줄 ID
     * @return 성공 응답
     */
    @DeleteMapping("/schedules/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteSchedule(@PathVariable Long scheduleId) {
        log.info("스케줄 삭제 요청: Schedule ID {}", scheduleId);
        scheduleService.deleteSchedule(scheduleId);
        return ApiResponse.success(null);
    }
}