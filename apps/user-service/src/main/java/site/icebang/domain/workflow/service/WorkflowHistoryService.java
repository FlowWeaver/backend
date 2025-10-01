package site.icebang.domain.workflow.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import site.icebang.common.dto.PageParamsDto;
import site.icebang.common.dto.PageResultDto;
import site.icebang.common.service.PageableService;
import site.icebang.domain.workflow.dto.JobRunDto;
import site.icebang.domain.workflow.dto.TaskRunDto;
import site.icebang.domain.workflow.dto.WorkflowHistoryDTO;
import site.icebang.domain.workflow.dto.WorkflowRunDetailResponseDto;
import site.icebang.domain.workflow.dto.WorkflowRunDto;
import site.icebang.domain.workflow.dto.WorkflowRunLogsResponseDto;
import site.icebang.domain.workflow.mapper.WorkflowHistoryMapper;

/**
 * 워크플로우 실행 이력(History) 조회 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 *
 * <p>이 서비스는 워크플로우 실행 목록의 페이징 처리, 특정 실행 건의 상세 정보 조회 등 읽기 전용(Read-Only) 기능에 집중합니다.
 *
 * <h2>주요 기능:</h2>
 *
 * <ul>
 *   <li>워크플로우 실행 이력 목록 페이징 조회
 *   <li>워크플로우 실행 상세 정보 조회 (Job 및 Task 실행 이력 포함)
 * </ul>
 *
 * @author jihu0210@naver.com
 * @since v0.1.0
 */
@Service
@RequiredArgsConstructor
public class WorkflowHistoryService implements PageableService<WorkflowHistoryDTO> {
  private final WorkflowHistoryMapper workflowHistoryMapper;

  /**
   * 워크플로우 실행 이력 목록을 페이징 처리하여 조회합니다.
   *
   * <p>이 메소드는 {@code PageableService} 인터페이스를 구현하며, {@code PageResult} 유틸리티를 사용하여 전체 카운트 쿼리와 목록 조회
   * 쿼리를 실행하고 페이징 결과를 생성합니다.
   *
   * @param pageParamsDto 페이징 처리에 필요한 파라미터 (페이지 번호, 페이지 크기 등)
   * @return 페이징 처리된 워크플로우 실행 이력 목록
   * @see PageResultDto
   * @since v0.1.0
   */
  @Override
  @Transactional(readOnly = true)
  public PageResultDto<WorkflowHistoryDTO> getPagedResult(PageParamsDto pageParamsDto) {

    return PageResultDto.from(
            pageParamsDto,
        () -> workflowHistoryMapper.selectWorkflowHistoryList(pageParamsDto),
        () -> workflowHistoryMapper.selectWorkflowHistoryCount(pageParamsDto));
  }

  /**
   * 특정 워크플로우 실행 건의 상세 정보를 조회합니다.
   *
   * <p>지정된 실행 ID(`runId`)에 해당하는 워크플로우 실행 정보와, 그에 속한 모든 Job 실행 정보, 그리고 각 Job에 속한 모든 Task 실행 정보를
   * 계층적으로 조회하여 반환합니다.
   *
   * @param runId 조회할 워크플로우 실행의 ID (`workflow_run.id`)
   * @return 워크플로우, Job, Task 실행 정보를 포함하는 상세 응답 객체
   * @since v0.1.0
   */
  @Transactional(readOnly = true)
  public WorkflowRunDetailResponseDto getWorkflowRunDetail(Long runId) {
    // 1. 워크플로우 실행 정보 조회
    WorkflowRunDto workflowRunDto = workflowHistoryMapper.selectWorkflowRun(runId);

    // 2. Job 실행 목록 조회
    List<JobRunDto> jobRunDtos = workflowHistoryMapper.selectJobRunsByWorkflowRunId(runId);

    // 3. 각 Job의 Task 실행 목록 조회
    if (jobRunDtos != null) {
      jobRunDtos.forEach(
          jobRun -> {
            List<TaskRunDto> taskRuns =
                workflowHistoryMapper.selectTaskRunsByJobRunId(jobRun.getId());
            jobRun.setTaskRuns(taskRuns);
          });
    }

    // 4. TraceId 조회
    String traceId = workflowHistoryMapper.selectTraceIdByRunId(runId);

    return WorkflowRunDetailResponseDto.builder()
        .workflowRun(workflowRunDto)
        .jobRuns(jobRunDtos)
        .traceId(traceId)
        .build();
  }

  /**
   * 특정 워크플로우 실행과 관련된 모든 로그를 조회합니다.
   *
   * @param runId 조회할 워크플로우 실행의 ID (`workflow_run.id`)
   * @return 워크플로우 실행 로그 응답 객체
   * @since v0.1.0
   */
  public WorkflowRunLogsResponseDto getWorkflowRunLogs(Long runId) {
    // TODO: 구현 예정
    return null;
  }

  /**
   * Trace ID를 사용하여 특정 워크플로우 실행 정보를 조회합니다.
   *
   * @param traceId 조회할 워크플로우 실행의 Trace ID (`workflow_run.trace_id`)
   * @return 워크플로우 실행 상세 응답 객체
   * @since v0.1.0
   */
  public WorkflowRunDetailResponseDto getWorkflowRunByTraceId(String traceId) {
    // TODO: 구현 예정
    return null;
  }
}
