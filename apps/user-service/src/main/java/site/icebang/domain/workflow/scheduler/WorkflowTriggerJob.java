package site.icebang.domain.workflow.scheduler;

import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import site.icebang.domain.workflow.service.RequestContextService;
import site.icebang.domain.workflow.service.WorkflowExecutionService;

/**
 * Spring Quartz 스케줄러에 의해 실행되는 실제 작업(Job) 클래스입니다.
 *
 * <p>이 클래스는 Quartz의 스케줄링 세계와 애플리케이션의 비즈니스 로직을 연결하는 **브릿지(Bridge)** 역할을 수행합니다. Quartz의 Trigger가 정해진
 * 시간에 발동하면, Quartz 엔진은 이 Job을 인스턴스화하고 {@code executeInternal} 메소드를 호출합니다.
 *
 * <h2>주요 기능:</h2>
 *
 * <ul>
 *   <li>스케줄 실행 시점에 {@code JobDataMap}에서 실행할 워크플로우 ID를 추출
 *   <li>추출된 ID를 사용하여 {@code WorkflowExecutionService}를 호출하여 실제 워크플로우 실행을 위임
 * </ul>
 *
 * @author jihu0210@naver.com
 * @since v0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTriggerJob extends QuartzJobBean {
  private final WorkflowExecutionService workflowExecutionService;
  private final RequestContextService requestContextService;

  /**
   * Quartz 스케줄러에 의해 트리거가 발동될 때 호출되는 메인 실행 메소드입니다.
   *
   * <p>이 메소드는 실행 컨텍스트({@code JobExecutionContext})에서 {@code JobDataMap}을 통해 스케줄 등록 시점에 저장된
   * 'workflowId'를 추출합니다. 그 후, 해당 ID를 파라미터로 하여 {@code WorkflowExecutionService}의 {@code
   * executeWorkflow} 메소드를 호출하여 실제 비즈니스 로직의 실행을 시작합니다.
   *
   * @param context Quartz가 제공하는 현재 실행에 대한 런타임 정보. JobDetail과 Trigger 정보를 포함합니다.
   * @since v0.1.0
   */
  @Override
  protected void executeInternal(JobExecutionContext context) {
    Long workflowId = context.getJobDetail().getJobDataMap().getLong("workflowId");
    log.info("Quartz가 WorkflowTriggerJob을 실행합니다. WorkflowId={}", workflowId);
    workflowExecutionService.executeWorkflow(workflowId, requestContextService.quartzContext());
  }
}
