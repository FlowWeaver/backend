package site.icebang.global.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import site.icebang.domain.workflow.dto.RequestContextDto;
import site.icebang.domain.workflow.mapper.WorkflowRunMapper;
import site.icebang.domain.workflow.model.WorkflowRun;
import site.icebang.domain.workflow.service.WorkflowExecutionService;

/**
 * 애플리케이션 시작 시, 비정상 종료된 워크플로우를 감지하고 '실제로 복구(재실행)'하는 클래스입니다.
 *
 * <p>서버가 시작될 때 DB에 'RUNNING' 상태로 남아있는 워크플로우는 시스템 장애(전원 차단, OOM 등)로 인해 중단된 작업입니다. 이 클래스는 해당 작업들을
 * 찾아내어 이전 기록을 정리하고, 작업을 <b>자동으로 재시작</b>합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowRecoveryInitializer implements ApplicationRunner {

  private final WorkflowRunMapper workflowRunMapper;
  private final WorkflowExecutionService workflowExecutionService;

  @Override
  public void run(ApplicationArguments args) {
    try {
      List<WorkflowRun> runningWorkflows = workflowRunMapper.findByStatus("RUNNING");

      if (runningWorkflows.isEmpty()) {
        return;
      }

      log.warn("비정상 종료된 워크플로우 {}건 발견. 자동 복구(재실행) 프로세스를 시작합니다.", runningWorkflows.size());

      int recoveredCount = 0;
      for (WorkflowRun run : runningWorkflows) {
        if (recoverAndRestart(run)) {
          recoveredCount++;
        }
      }
      log.info("총 {}건의 워크플로우가 성공적으로 복구(재실행 요청)되었습니다.", recoveredCount);

    } catch (Exception e) {
      log.error("워크플로우 복구 프로세스 전체 실패", e);
    }
  }

  private boolean recoverAndRestart(WorkflowRun run) {
    try {
      // 1. 기존 실행 이력 마감 (Cleaning)
      // 중단된 시점의 상태를 FAILED로 확정지어 데이터 정합성을 맞춥니다.
      run.finish("FAILED");
      workflowRunMapper.update(run);
      log.info("[복구 1단계] 기존 이력 정리 완료: RunID={}, WorkflowID={}", run.getId(), run.getWorkflowId());

      // 2. 워크플로우 재실행 (Restarting)
      // 기존 Trace ID를 승계하여 로그 추적성을 유지하며 서비스를 다시 시작합니다.
      RequestContextDto recoveryContext = RequestContextDto.forRecovery(run.getTraceId());

      workflowExecutionService.executeWorkflow(run.getWorkflowId(), recoveryContext);
      log.info(
          "[복구 2단계] 워크플로우 재실행 요청 완료: WorkflowID={}, TraceID={}",
          run.getWorkflowId(),
          run.getTraceId());

      return true;
    } catch (Exception e) {
      log.error("워크플로우 개별 복구 실패: RunID={}", run.getId(), e);
      return false;
    }
  }
}
