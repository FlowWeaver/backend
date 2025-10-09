package site.icebang.domain.workflow.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import site.icebang.common.dto.ApiResponseDto;
import site.icebang.common.dto.PageParamsDto;
import site.icebang.common.dto.PageResultDto;
import site.icebang.domain.log.service.ExecutionLogService;
import site.icebang.domain.workflow.dto.WorkflowHistoryDTO;
import site.icebang.domain.workflow.dto.WorkflowRunDetailResponseDto;
import site.icebang.domain.workflow.dto.log.ExecutionLogSimpleDto;
import site.icebang.domain.workflow.dto.log.WorkflowLogQueryCriteriaDto;
import site.icebang.domain.workflow.service.WorkflowHistoryService;

@RestController
@RequestMapping("/v0/workflow-runs")
@RequiredArgsConstructor
public class WorkflowHistoryController {
  private final WorkflowHistoryService workflowHistoryService;
  private final ExecutionLogService executionLogService;

  @GetMapping("")
  public ApiResponseDto<PageResultDto<WorkflowHistoryDTO>> getWorkflowHistoryList(
      @ModelAttribute PageParamsDto pageParamsDto) {
    PageResultDto<WorkflowHistoryDTO> response =
        workflowHistoryService.getPagedResult(pageParamsDto);
    return ApiResponseDto.success(response);
  }

  /**
   * 워크플로우 실행 상세 조회
   *
   * @param runId workflow_run.id
   * @return WorkflowRunDetailResponse
   */
  @GetMapping("/{runId}")
  public ApiResponseDto<WorkflowRunDetailResponseDto> getWorkflowRunDetail(
      @PathVariable Long runId) {
    WorkflowRunDetailResponseDto response = workflowHistoryService.getWorkflowRunDetail(runId);
    return ApiResponseDto.success(response);
  }

  @GetMapping("/logs")
  public ApiResponseDto<List<ExecutionLogSimpleDto>> getTaskExecutionLog(
      @Valid @ModelAttribute WorkflowLogQueryCriteriaDto requestDto) {
    return ApiResponseDto.success(
        ExecutionLogSimpleDto.from(executionLogService.getRawLogs(requestDto)));
  }
}
