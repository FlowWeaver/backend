package site.icebang.domain.workflow.service;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

import site.icebang.domain.workflow.dto.JobDto;
import site.icebang.domain.workflow.dto.RequestContext;
import site.icebang.domain.workflow.dto.TaskDto;
import site.icebang.domain.workflow.dto.WorkflowDetailCardDto;
import site.icebang.domain.workflow.manager.ExecutionMdcManager;
import site.icebang.domain.workflow.mapper.JobMapper;
import site.icebang.domain.workflow.mapper.JobRunMapper;
import site.icebang.domain.workflow.mapper.TaskIoDataMapper;
import site.icebang.domain.workflow.mapper.TaskRunMapper;
import site.icebang.domain.workflow.mapper.WorkflowMapper;
import site.icebang.domain.workflow.mapper.WorkflowRunMapper;
import site.icebang.domain.workflow.model.Job;
import site.icebang.domain.workflow.model.JobRun;
import site.icebang.domain.workflow.model.Task;
import site.icebang.domain.workflow.model.TaskIoData;
import site.icebang.domain.workflow.model.TaskRun;
import site.icebang.domain.workflow.model.WorkflowRun;
import site.icebang.domain.workflow.runner.TaskRunner;
import site.icebang.domain.workflow.runner.fastapi.body.TaskBodyBuilder;

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
  public void executeWorkflow(Long workflowId, RequestContext context) {
    WorkflowRun workflowRun = WorkflowRun.start(workflowId, context.getTraceId());
    workflowRunMapper.insert(workflowRun);

    mdcManager.setWorkflowContext(
        workflowId, context.getTraceId(), context.getClientIp(), context.getUserAgent());
    try {
      workflowLogger.info("========== 워크플로우 실행 시작: WorkflowId={} ==========", workflowId);

      // 📌 1. selectWorkflowDetailById를 호출하여 워크플로우의 모든 상세 정보를 가져옵니다.
      WorkflowDetailCardDto settingsDto =
          workflowMapper.selectWorkflowDetailById(BigInteger.valueOf(workflowId));
      if (settingsDto == null) {
        throw new IllegalStateException("실행할 워크플로우를 찾을 수 없습니다: ID " + workflowId);
      }

      // 📌 2. 가져온 DTO 객체에서 getDefaultConfig() 메소드를 호출하여 값을 얻습니다.
      String defaultConfigJson = settingsDto.getDefaultConfig();
      JsonNode setting =
          (defaultConfigJson != null && !defaultConfigJson.isEmpty())
              ? objectMapper.readTree(defaultConfigJson)
              : objectMapper.createObjectNode();

      List<JobDto> jobDtos = jobMapper.findJobsByWorkflowId(workflowId);
      jobDtos.sort(
          Comparator.comparing(
                  JobDto::getExecutionOrder, Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(JobDto::getId));

      boolean hasAnyJobFailed = false;

      for (JobDto jobDto : jobDtos) {
        Job job = new Job(jobDto);
        mdcManager.setJobContext(job.getId());
        JobRun jobRun = JobRun.start(workflowRun.getId(), job.getId());
        jobRunMapper.insert(jobRun);
        workflowLogger.info(
            "---------- Job 실행 시작: JobId={}, JobRunId={} ----------", job.getId(), jobRun.getId());

        boolean jobSucceeded = executeTasksForJob(jobRun, setting);
        jobRun.finish(jobSucceeded ? "SUCCESS" : "FAILED");
        jobRunMapper.update(jobRun);

        if (!jobSucceeded) {
          hasAnyJobFailed = true;
        }
        mdcManager.setWorkflowContext(
            workflowId, context.getTraceId(), context.getClientIp(), context.getUserAgent());
      }
      workflowRun.finish(hasAnyJobFailed ? "FAILED" : "SUCCESS");
      workflowRunMapper.update(workflowRun);
      workflowLogger.info(
          "========== 워크플로우 실행 {} : WorkflowRunId={} ==========",
          hasAnyJobFailed ? "실패" : "성공",
          workflowRun.getId());
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
    taskDtos.forEach(
        dto -> {
          JsonNode s = setting.get(String.valueOf(dto.getId()));
          if (s != null) dto.setSettings(s);
        });
    taskDtos.sort(
        Comparator.comparing(
                TaskDto::getExecutionOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TaskDto::getId));

    boolean hasAnyTaskFailed = false;
    Long s3UploadTaskRunId = null; // S3 업로드 태스크의 task_run_id 저장용

    for (TaskDto taskDto : taskDtos) {
      TaskRun taskRun = null;
      try {
        taskRun = TaskRun.start(jobRun.getId(), taskDto.getId(), taskDto.getExecutionOrder());
        taskRunMapper.insert(taskRun);
        mdcManager.setTaskContext(taskRun.getId());

        Task task = new Task(taskDto);
        workflowLogger.info("Task 실행 시작: TaskId={}, Name={}", task.getId(), task.getName());

        ObjectNode requestBody =
            bodyBuilders.stream()
                .filter(builder -> builder.supports(task.getName()))
                .findFirst()
                .map(builder -> builder.build(task, jobRun))
                .orElse(objectMapper.createObjectNode());

        // TODO: 아래 로직 다른 곳으로 분리시키기
        if ("S3 업로드 태스크".equals(task.getName())) {
          requestBody.put("task_run_id", taskRun.getId());
          s3UploadTaskRunId = taskRun.getId(); // S3 업로드의 task_run_id 저장
        } else if ("상품 선택 태스크".equals(task.getName())) {
          // S3 업로드에서 사용한 task_run_id를 사용
          if (s3UploadTaskRunId != null) {
            requestBody.put("task_run_id", s3UploadTaskRunId);
          } else {
            workflowLogger.error("S3 업로드 태스크가 먼저 실행되지 않아 task_run_id를 찾을 수 없습니다.");
            // 또는 이전 Job에서 S3 업로드를 찾는 로직 추가 가능
          }
        }

        saveIoData(taskRun.getId(), "INPUT", "request_body", requestBody);
        TaskRunner.TaskExecutionResult result =
            taskExecutionService.executeWithRetry(task, taskRun, requestBody);
        taskRun.finish(result.status(), result.message());

        if (result.isFailure()) {
          hasAnyTaskFailed = true;
          saveIoData(
              taskRun.getId(),
              "OUTPUT",
              "error_message",
              objectMapper.valueToTree(result.message()));
        } else {
          JsonNode resultJson = objectMapper.readTree(result.message());
          saveIoData(taskRun.getId(), "OUTPUT", "response_body", resultJson);
        }
      } catch (Exception e) {
        workflowLogger.error(
            "Task 처리 중 심각한 오류 발생: JobRunId={}, TaskName={}", jobRun.getId(), taskDto.getName(), e);
        hasAnyTaskFailed = true;
        if (taskRun != null) {
          taskRun.finish("FAILED", e.getMessage());
          saveIoData(
              taskRun.getId(), "OUTPUT", "error_message", objectMapper.valueToTree(e.getMessage()));
        }
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
      TaskIoData ioData =
          new TaskIoData(
              taskRunId, ioType, name, "JSON", dataValue, (long) dataValue.getBytes().length);
      taskIoDataMapper.insert(ioData);
    } catch (Exception e) {
      workflowLogger.error("Task IO 데이터 저장 실패: TaskRunId={}, Type={}", taskRunId, ioType, e);
    }
  }
}
