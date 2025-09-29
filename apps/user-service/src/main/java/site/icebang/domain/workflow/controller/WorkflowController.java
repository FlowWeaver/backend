package site.icebang.domain.workflow.controller;

import java.math.BigInteger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import site.icebang.common.dto.ApiResponse;
import site.icebang.common.dto.PageParams;
import site.icebang.common.dto.PageResult;
import site.icebang.domain.auth.model.AuthCredential;
import site.icebang.domain.workflow.dto.RequestContext;
import site.icebang.domain.workflow.dto.WorkflowCardDto;
import site.icebang.domain.workflow.dto.WorkflowCreateDto;
import site.icebang.domain.workflow.dto.WorkflowDetailCardDto;
import site.icebang.domain.workflow.service.RequestContextService;
import site.icebang.domain.workflow.service.WorkflowExecutionService;
import site.icebang.domain.workflow.service.WorkflowService;

@RestController
@RequestMapping("/v0/workflows")
@RequiredArgsConstructor
public class WorkflowController {
  private final WorkflowService workflowService;
  private final WorkflowExecutionService workflowExecutionService;
  private final RequestContextService requestContextService;

  @GetMapping("")
  public ApiResponse<PageResult<WorkflowCardDto>> getWorkflowList(
      @ModelAttribute PageParams pageParams) {
    PageResult<WorkflowCardDto> result = workflowService.getPagedResult(pageParams);
    return ApiResponse.success(result);
  }

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Void> createWorkflow(
      @Valid @RequestBody WorkflowCreateDto workflowCreateDto,
      @AuthenticationPrincipal AuthCredential authCredential) {
    // 인증 체크
    if (authCredential == null) {
      throw new IllegalArgumentException("로그인이 필요합니다");
    }

    // AuthCredential에서 userId 추출
    BigInteger userId = authCredential.getId();

    workflowService.createWorkflow(workflowCreateDto, userId);
    return ApiResponse.success(null);
  }

  @PostMapping("/{workflowId}/run")
  public ResponseEntity<Void> runWorkflow(@PathVariable Long workflowId) {

    RequestContext context = requestContextService.extractRequestContext();
    // HTTP 요청/응답 스레드를 블로킹하지 않도록 비동기 실행
    workflowExecutionService.executeWorkflow(workflowId, context);
    return ResponseEntity.accepted().build();
  }

  @GetMapping("/{workflowId}/detail")
  public ApiResponse<WorkflowDetailCardDto> getWorkflowDetail(@PathVariable BigInteger workflowId) {
    WorkflowDetailCardDto result = workflowService.getWorkflowDetail(workflowId);
    return ApiResponse.success(result);
  }

  /**
   * 워크플로우를 삭제합니다 (논리 삭제).
   *
   * <p>워크플로우를 비활성화하고 모든 스케줄을 중단합니다.
   *
   * @param workflowId 삭제할 워크플로우 ID
   * @return 성공 응답
   */
  @DeleteMapping("/{workflowId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ApiResponse<Void> deleteWorkflow(@PathVariable BigInteger workflowId) {
    workflowService.deleteWorkflow(workflowId);
    return ApiResponse.success(null);
  }

  /**
   * 워크플로우를 비활성화합니다.
   *
   * <p>워크플로우를 중단하고 모든 스케줄을 Quartz에서 제거합니다.
   *
   * @param workflowId 비활성화할 워크플로우 ID
   * @return 성공 응답
   */
  @PatchMapping("/{workflowId}/deactivate")
  public ApiResponse<Void> deactivateWorkflow(@PathVariable BigInteger workflowId) {
    workflowService.deactivateWorkflow(workflowId);
    return ApiResponse.success(null);
  }

  /**
   * 워크플로우를 활성화합니다.
   *
   * <p>워크플로우를 재개하고 모든 활성 스케줄을 Quartz에 재등록합니다.
   *
   * @param workflowId 활성화할 워크플로우 ID
   * @return 성공 응답
   */
  @PatchMapping("/{workflowId}/activate")
  public ApiResponse<Void> activateWorkflow(@PathVariable BigInteger workflowId) {
    workflowService.activateWorkflow(workflowId);
    return ApiResponse.success(null);
  }

  /**
   * 워크플로우의 특정 스케줄을 삭제합니다.
   *
   * <p>스케줄을 비활성화하고 Quartz에서 제거합니다.
   *
   * @param workflowId 워크플로우 ID
   * @param scheduleId 삭제할 스케줄 ID
   * @return 성공 응답
   */
  @DeleteMapping("/{workflowId}/schedules/{scheduleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ApiResponse<Void> deleteWorkflowSchedule(
      @PathVariable BigInteger workflowId, @PathVariable Long scheduleId) {
    workflowService.deleteWorkflowSchedule(workflowId, scheduleId);
    return ApiResponse.success(null);
  }
}
