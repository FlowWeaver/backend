package site.icebang.domain.workflow.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import site.icebang.common.dto.PageParamsDto;
import site.icebang.domain.workflow.dto.JobRunDto;
import site.icebang.domain.workflow.dto.TaskRunDto;
import site.icebang.domain.workflow.dto.WorkflowHistoryDTO;
import site.icebang.domain.workflow.dto.WorkflowRunDto;

/**
 * 워크플로우 실행 히스토리 관련 데이터베이스 매퍼 인터페이스입니다.
 *
 * <p>워크플로우, 작업(Job), 태스크(Task)의 실행 기록과 관련된 데이터 조회를 담당합니다.
 *
 * @author jys01012@gmail.com
 * @since v0.0.1-beta
 */
@Mapper
public interface WorkflowHistoryMapper {
  /**
   * 워크플로우 실행 정보를 조회합니다.
   *
   * @param runId workflow_run.id
   * @return 워크플로우 실행 정보
   */
  WorkflowRunDto selectWorkflowRun(Long runId);

  /**
   * 워크플로우 실행의 작업 목록을 조회합니다.
   *
   * @param workflowRunId workflow_run.id
   * @return 작업 실행 정보 목록
   */
  List<JobRunDto> selectJobRunsByWorkflowRunId(Long workflowRunId);

  /**
   * 작업 실행의 태스크 목록을 조회합니다.
   *
   * @param jobRunId job_run.id
   * @return 태스크 실행 정보 목록
   */
  List<TaskRunDto> selectTaskRunsByJobRunId(Long jobRunId);

  /**
   * 워크플로우 실행 TraceId를 조회합니다.
   *
   * @param runId workflow_run.id
   * @return 추적 ID 문자열
   */
  String selectTraceIdByRunId(Long runId);

  /**
   * 페이지네이션을 적용한 워크플로우 히스토리 목록을 조회합니다.
   *
   * @param pageParamsDto 페이지 매개변수
   * @return 워크플로우 히스토리 정보 목록
   */
  List<WorkflowHistoryDTO> selectWorkflowHistoryList(PageParamsDto pageParamsDto);

  /**
   * 워크플로우 런 인스턴스의 총 개수를 조회합니다.
   *
   * @param pageParamsDto 페이지 매개변수
   * @return 총 결과 개수
   */
  int selectWorkflowHistoryCount(PageParamsDto pageParamsDto);
}
