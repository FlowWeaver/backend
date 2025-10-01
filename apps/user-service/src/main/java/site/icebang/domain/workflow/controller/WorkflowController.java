package site.icebang.domain.workflow.controller;

import java.math.BigInteger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import site.icebang.common.dto.ApiResponseDto;
import site.icebang.common.dto.PageParamsDto;
import site.icebang.common.dto.PageResultDto;
import site.icebang.domain.auth.model.AuthCredential;
import site.icebang.domain.workflow.dto.RequestContextDto;
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
  public ApiResponseDto<PageResultDto<WorkflowCardDto>> getWorkflowList(
      @ModelAttribute PageParamsDto pageParamsDto) {
    PageResultDto<WorkflowCardDto> result = workflowService.getPagedResult(pageParamsDto);
    return ApiResponseDto.success(result);
  }

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponseDto<Void> createWorkflow(
      @Valid @RequestBody WorkflowCreateDto workflowCreateDto,
      @AuthenticationPrincipal AuthCredential authCredential) {
    // 인증 체크
    if (authCredential == null) {
      throw new IllegalArgumentException("로그인이 필요합니다");
    }

    // AuthCredential에서 userId 추출
    BigInteger userId = authCredential.getId();

    workflowService.createWorkflow(workflowCreateDto, userId);
    return ApiResponseDto.success(null);
  }

  @PostMapping("/{workflowId}/run")
  public ResponseEntity<Void> runWorkflow(@PathVariable Long workflowId) {

    RequestContextDto context = requestContextService.extractRequestContext();
    // HTTP 요청/응답 스레드를 블로킹하지 않도록 비동기 실행
    workflowExecutionService.executeWorkflow(workflowId, context);
    return ResponseEntity.accepted().build();
  }

  @GetMapping("/{workflowId}/detail")
  public ApiResponseDto<WorkflowDetailCardDto> getWorkflowDetail(@PathVariable BigInteger workflowId) {
    WorkflowDetailCardDto result = workflowService.getWorkflowDetail(workflowId);
    return ApiResponseDto.success(result);
  }
}
