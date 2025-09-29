package site.icebang.domain.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.icebang.domain.workflow.mapper.JobRunMapper;
import site.icebang.domain.workflow.mapper.TaskIoDataMapper;
import site.icebang.domain.workflow.mapper.TaskRunMapper;
import site.icebang.domain.workflow.mapper.WorkflowRunMapper;
import site.icebang.domain.workflow.model.JobRun;
import site.icebang.domain.workflow.model.TaskIoData;
import site.icebang.domain.workflow.model.TaskRun;
import site.icebang.domain.workflow.model.WorkflowRun;
import site.icebang.domain.workflow.dto.JobDto;
import site.icebang.domain.workflow.dto.TaskDto;
import site.icebang.domain.workflow.dto.WorkflowDetailCardDto;
import site.icebang.domain.workflow.manager.ExecutionMdcManager;
import site.icebang.domain.workflow.mapper.JobMapper;
import site.icebang.domain.workflow.mapper.WorkflowMapper;
import site.icebang.domain.workflow.model.Job;
import site.icebang.domain.workflow.model.Task;
import site.icebang.domain.workflow.runner.TaskRunner;
import site.icebang.domain.workflow.runner.fastapi.body.TaskBodyBuilder;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {
  private static final Logger workflowLogger = LoggerFactory.getLogger("WORKFLOW_HISTORY");
  private final JobMapper jobMapper;
  private final WorkflowRunMapper workflowRunMapper;
  private final JobRunMapper jobRunMapper;
  private final TaskRunMapper taskRunMapper;
  private final TaskIoDataMapper taskIoDataMapper;
  private final ObjectMapper objectMapper;
  private final List<TaskBodyBuilder> bodyBuilders;
  private final ExecutionMdcManager mdcManager;
  private final TaskExecutionService taskExecutionService;
  private final WorkflowMapper workflowMapper;

  @Transactional
  @Async("traceExecutor")
  public void executeWorkflow(Long workflowId) {
    mdcManager.setWorkflowContext(workflowId);
    WorkflowRun workflowRun = null;
    try {
      workflowLogger.info("========== 워크플로우 실행 시작: WorkflowId={} ==========", workflowId);
      workflowRun = WorkflowRun.start(workflowId);
      workflowRunMapper.insert(workflowRun);

      // 📌 1. 워크플로우 상세 정보를 DTO로 조회합니다.
      WorkflowDetailCardDto settingsDto = workflowMapper.selectWorkflowDetailById(BigInteger.valueOf(workflowId));
      if (settingsDto == null) {
        throw new IllegalStateException("실행할 워크플로우를 찾을 수 없습니다: ID " + workflowId);
      }

      // 📌 2. DTO에서 defaultConfig JSON 문자열을 가져와 파싱합니다.
      String defaultConfigJson = settingsDto.getDefaultConfig();
      JsonNode setting = (defaultConfigJson != null && !defaultConfigJson.isEmpty())
              ? objectMapper.readTree(defaultConfigJson)
              : objectMapper.createObjectNode();

      List<JobDto> jobDtos = jobMapper.findJobsByWorkflowId(workflowId);
      jobDtos.sort(Comparator.comparing(JobDto::getExecutionOrder, Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(JobDto::getId));

      boolean hasAnyJobFailed = false;

      for (JobDto jobDto : jobDtos) {
        Job job = new Job(jobDto);
        mdcManager.setJobContext(job.getId());
        JobRun jobRun = JobRun.start(workflowRun.getId(), job.getId());
        jobRunMapper.insert(jobRun);
        workflowLogger.info("---------- Job 실행 시작: JobId={}, JobRunId={} ----------", job.getId(), jobRun.getId());

        boolean jobSucceeded = executeTasksForJob(jobRun, setting);
        jobRun.finish(jobSucceeded ? "SUCCESS" : "FAILED");
        jobRunMapper.update(jobRun);

        if (!jobSucceeded) hasAnyJobFailed = true;

        mdcManager.setWorkflowContext(workflowId);
      }
      workflowRun.finish(hasAnyJobFailed ? "FAILED" : "SUCCESS");
      workflowRunMapper.update(workflowRun);
      workflowLogger.info("========== 워크플로우 실행 {} : ...", hasAnyJobFailed ? "실패" : "성공");
    } catch (Exception e) {
      workflowLogger.error("워크플로우 실행 중 심각한 오류 발생: WorkflowId={}", workflowId, e);
      if (workflowRun != null) {
        workflowRun.finish("FAILED");
        workflowRunMapper.update(workflowRun);
      }
    } finally {
      mdcManager.clearExecutionContext();
    }
  }

  private boolean executeTasksForJob(JobRun jobRun, JsonNode setting) {
    List<TaskDto> taskDtos = jobMapper.findTasksByJobId(jobRun.getJobId());
    taskDtos.forEach(dto -> {
      JsonNode s = setting.get(String.valueOf(dto.getId()));
      if (s != null) dto.setSettings(s);
    });
    taskDtos.sort(Comparator.comparing(TaskDto::getExecutionOrder, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(TaskDto::getId));

    boolean hasAnyTaskFailed = false;

    for (TaskDto taskDto : taskDtos) {
      TaskRun taskRun = null;
      try {
        taskRun = TaskRun.start(jobRun.getId(), taskDto.getId(), taskDto.getExecutionOrder());
        taskRunMapper.insert(taskRun);
        mdcManager.setTaskContext(taskRun.getId());
        workflowLogger.info("Task 실행 시작: TaskId={}, Name={}", taskDto.getId(), taskDto.getName());

        Task task = new Task(taskDto);

        ObjectNode requestBody = bodyBuilders.stream()
                .filter(builder -> builder.supports(task.getName()))
                .findFirst()
                .map(builder -> builder.build(task, jobRun)) // jobRun을 컨텍스트로 전달
                .orElse(objectMapper.createObjectNode());

        saveIoData(taskRun.getId(), "INPUT", "request_body", requestBody);
        TaskRunner.TaskExecutionResult result = taskExecutionService.executeWithRetry(task, taskRun, requestBody);
        taskRun.finish(result.status(), result.message());

        if (result.isFailure()) {
          hasAnyTaskFailed = true;
        } else {
          JsonNode resultJson = objectMapper.readTree(result.message());
          saveIoData(taskRun.getId(), "OUTPUT", "response_body", resultJson);
        }
      } catch (Exception e) {
        workflowLogger.error("Task 처리 중 심각한 오류 발생: JobRunId={}, TaskName={}", jobRun.getId(), taskDto.getName(), e);
        hasAnyTaskFailed = true;
        if (taskRun != null) taskRun.finish("FAILED", e.getMessage());
      } finally {
        if (taskRun != null) taskRunMapper.update(taskRun);
        mdcManager.setJobContext(jobRun.getId());
      }
    }
    return !hasAnyTaskFailed;
  }

  private void saveIoData(Long taskRunId, String ioType, String name, JsonNode data) {
    try {
      String dataValue = data.toString();
      TaskIoData ioData = new TaskIoData(taskRunId, ioType, name, "JSON", dataValue, (long) dataValue.getBytes().length);
      taskIoDataMapper.insert(ioData);
    } catch (Exception e) {
      workflowLogger.error("Task IO 데이터 저장 실패: TaskRunId={}, Type={}", taskRunId, ioType, e);
    }
  }
}