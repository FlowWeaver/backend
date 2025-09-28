package site.icebang.domain.workflow.model;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.MDC;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WorkflowRun {

  private Long id;
  private Long workflowId;
  private String traceId; // 분산 추적을 위한 ID
  private String status; // PENDING, RUNNING, SUCCESS, FAILED
  private Instant startedAt;
  private Instant finishedAt;
  private Instant createdAt;

  private WorkflowRun(Long workflowId, String traceId) {
    this.workflowId = workflowId;
    // MDC에서 현재 요청의 traceId를 가져오거나, 없으면 새로 생성
    this.traceId = traceId;
    this.status = "RUNNING";
    this.startedAt = Instant.now();
    this.createdAt = this.startedAt;
  }

  /** 워크플로우 실행 시작을 위한 정적 팩토리 메소드 */
  public static WorkflowRun start(Long workflowId, String traceId) {
    return new WorkflowRun(workflowId, traceId);
  }

  /** 워크플로우 실행 완료 처리 */
  public void finish(String status) {
    this.status = status;
    this.finishedAt = Instant.now();
  }
}
