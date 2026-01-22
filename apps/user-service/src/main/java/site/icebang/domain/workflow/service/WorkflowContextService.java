package site.icebang.domain.workflow.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import site.icebang.domain.workflow.mapper.TaskIoDataMapper;
import site.icebang.domain.workflow.mapper.TaskRunMapper;
import site.icebang.domain.workflow.model.JobRun;
import site.icebang.domain.workflow.model.TaskIoData;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowContextService {

  private final TaskRunMapper taskRunMapper;
  private final TaskIoDataMapper taskIoDataMapper;
  private final ObjectMapper objectMapper;

  /**
   * 전체 워크플로우 실행 범위(WorkflowRun) 내에서, 이전에 성공한 Task의 이름으로 결과(Output)를 조회합니다. Resume(이어하기) 시, 이전 Job이
   * 스킵되더라도 DB에서 전체 이력을 조회하여 데이터를 가져옵니다.
   *
   * @param jobRun 현재 실행중인 JobRun (내부의 workflowRunId를 사용하여 전체 범위 조회)
   * @param sourceTaskName 결과를 조회할 이전 Task의 이름
   * @return 조회된 결과 데이터 (JsonNode)
   */
  public Optional<JsonNode> getPreviousTaskOutput(JobRun jobRun, String sourceTaskName) {
    Long workflowRunId = jobRun.getWorkflowRunId();
    try {
      return Optional.ofNullable(
              taskRunMapper.findSuccessfulTaskRunByWorkflowRunId(workflowRunId, sourceTaskName))
          .flatMap(taskRun -> taskIoDataMapper.findOutputByTaskRunId(taskRun.getId()))
          .map(this::parseJson);
    } catch (Exception e) {
      log.error("워크플로우 데이터 조회 실패: WorkflowRunId={}, TaskName={}", workflowRunId, sourceTaskName, e);
      return Optional.empty();
    }
  }

  private JsonNode parseJson(TaskIoData ioData) {
    try {
      return objectMapper.readTree(ioData.getDataValue());
    } catch (Exception e) {
      log.error("TaskIoData JSON 파싱 실패: TaskIoDataId={}", ioData.getId(), e);
      return null;
    }
  }
}
