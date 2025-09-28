package site.icebang.domain.workflow.service;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

import site.icebang.domain.workflow.dto.JobDto;
import site.icebang.domain.workflow.dto.RequestContext;
import site.icebang.domain.workflow.dto.TaskDto;
import site.icebang.domain.workflow.dto.WorkflowDetailCardDto;
import site.icebang.domain.workflow.manager.ExecutionMdcManager;
import site.icebang.domain.workflow.mapper.*;
import site.icebang.domain.workflow.model.Job;
import site.icebang.domain.workflow.model.JobRun;
import site.icebang.domain.workflow.model.Task;
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
  private final ObjectMapper objectMapper;
  private final List<TaskBodyBuilder> bodyBuilders;
  private final ExecutionMdcManager mdcManager;
  private final TaskExecutionService taskExecutionService;
  private final WorkflowMapper workflowMapper;

  @Async("traceExecutor")
  public void executeWorkflow(Long workflowId, RequestContext context) {
    WorkflowRun workflowRun = WorkflowRun.start(workflowId, context.getTraceId());
    workflowRunMapper.insert(workflowRun);

    mdcManager.setWorkflowContext(workflowId, context.getTraceId(), context.getClientIp(), context.getUserAgent());
    try {
      workflowLogger.info("========== 워크플로우 실행 시작: WorkflowId={} ==========", workflowId);

      Map<String, JsonNode> workflowContext = new HashMap<>();
      WorkflowDetailCardDto settings =
          workflowMapper.selectWorkflowDetailById(BigInteger.valueOf(workflowId));
      workflowLogger.info("Workflow 정보 로드 성공");

      workflowLogger.info("Default config 로드 시도");
      JsonNode setting = objectMapper.readTree(settings.getDefaultConfig());
      workflowLogger.info("Default config 로드 성공");

      workflowLogger.info("Job 목록 로드 시도");
      List<JobDto> jobDtos = jobMapper.findJobsByWorkflowId(workflowId);
      workflowLogger.info("Job 목록 로드 성공");

      workflowLogger.info("execution_order 기준으로 정렬 시도");
      jobDtos.sort(
          Comparator.comparing(
                  JobDto::getExecutionOrder, Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(JobDto::getId));

      workflowLogger.info("execution_order 기준으로 성공");

      workflowLogger.info("총 {}개의 Job을 순차적으로 실행합니다.", jobDtos.size());
      boolean hasAnyJobFailed = false;

      // 📌 정렬된 JobDto 리스트를 순회합니다.
      for (JobDto jobDto : jobDtos) {
        // 📌 DTO로부터 Job 모델을 생성합니다.
        Job job = new Job(jobDto);

        mdcManager.setJobContext(job.getId());
        JobRun jobRun = JobRun.start(workflowRun.getId(), job.getId());
        jobRunMapper.insert(jobRun);
        workflowLogger.info(
            "---------- Job 실행 시작: JobId={}, JobRunId={} ----------", job.getId(), jobRun.getId());

        boolean jobSucceeded = executeTasksForJob(jobRun, workflowContext, setting);
        jobRun.finish(jobSucceeded ? "SUCCESS" : "FAILED");
        jobRunMapper.update(jobRun);

        if (!jobSucceeded) {
          workflowLogger.error("Job 실행 실패: JobRunId={}", jobRun.getId());
          hasAnyJobFailed = true;
        } else {
          workflowLogger.info("---------- Job 실행 성공: JobRunId={} ----------", jobRun.getId());
        }
        mdcManager.setWorkflowContext(workflowId);
      }
      workflowRun.finish(hasAnyJobFailed ? "FAILED" : "SUCCESS");
      workflowRunMapper.update(workflowRun);
      workflowLogger.info(
          "========== 워크플로우 실행 {} : WorkflowRunId={} ==========",
          hasAnyJobFailed ? "실패" : "성공",
          workflowRun.getId());
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    } finally {
      mdcManager.clearExecutionContext();
    }
  }

  private boolean executeTasksForJob(
      JobRun jobRun, Map<String, JsonNode> workflowContext, JsonNode setting) {
    List<TaskDto> taskDtos = jobMapper.findTasksByJobId(jobRun.getJobId());
    for (TaskDto taskDto : taskDtos) {
      String taskId = taskDto.getId().toString();
      JsonNode settingForTask = setting.get(taskId);
      if (settingForTask != null) {
        taskDto.setSettings(settingForTask);
      }
    }
    taskDtos.sort(
        Comparator.comparing(
                TaskDto::getExecutionOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TaskDto::getId));

    workflowLogger.info(
        "Job (JobRunId={}) 내 총 {}개의 Task를 순차 실행합니다.", jobRun.getId(), taskDtos.size());
    boolean hasAnyTaskFailed = false;
    Long s3UploadTaskRunId = null; // S3 업로드 태스크의 task_run_id 저장용

    for (TaskDto taskDto : taskDtos) {
      try {
        TaskRun taskRun =
            TaskRun.start(jobRun.getId(), taskDto.getId(), taskDto.getExecutionOrder());
        taskRunMapper.insert(taskRun);
        mdcManager.setTaskContext(taskRun.getId());
        workflowLogger.info("Task 실행 시작: TaskId={}, Name={}", taskDto.getId(), taskDto.getName());

        Task task = new Task(taskDto);

        ObjectNode requestBody =
            bodyBuilders.stream()
                .filter(builder -> builder.supports(task.getName()))
                .findFirst()
                .map(builder -> builder.build(task, workflowContext))
                .orElse(objectMapper.createObjectNode());

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

        TaskRunner.TaskExecutionResult result =
            taskExecutionService.executeWithRetry(task, taskRun, requestBody);
        taskRun.finish(result.status(), result.message());
        taskRunMapper.update(taskRun);

        if (result.isFailure()) {
          workflowLogger.error(
              "Task 최종 실패: TaskRunId={}, Message={}", taskRun.getId(), result.message());
          hasAnyTaskFailed = true;
        } else {
          JsonNode resultJson = objectMapper.readTree(result.message());
          workflowContext.put(task.getName(), resultJson);
          workflowLogger.info("Task 실행 성공: TaskRunId={}", taskRun.getId());
        }
      } catch (Exception e) {
        workflowLogger.error(
            "Task 처리 중 심각한 오류 발생: JobRunId={}, TaskName={}", jobRun.getId(), taskDto.getName(), e);
        hasAnyTaskFailed = true;
      } finally {
        mdcManager.setJobContext(jobRun.getId());
      }
    }
    return !hasAnyTaskFailed;
  }
}
