package site.icebang.domain.schedule.service;

import java.util.List;
import java.util.Set;

import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import site.icebang.domain.schedule.model.Schedule;
import site.icebang.domain.workflow.scheduler.WorkflowTriggerJob;

/**
 * Spring Quartz 스케줄러의 Job과 Trigger를 동적으로 관리하는 서비스 클래스입니다.
 *
 * <p>이 서비스는 데이터베이스에 정의된 {@code Schedule} 정보를 바탕으로, Quartz 엔진에 실제 실행 가능한 작업을 등록, 수정, 삭제하는
 * 역할을 담당합니다.
 *
 * <h2>주요 기능:</h2>
 *
 * <ul>
 *   <li>DB의 스케줄 정보를 바탕으로 Quartz Job 및 Trigger 생성 또는 업데이트
 *   <li>기존에 등록된 Quartz 스케줄 삭제
 *   <li>워크플로우의 모든 스케줄 일괄 삭제
 *   <li>Quartz 클러스터 환경에서 안전한 동작 보장
 * </ul>
 *
 * @author bwnfo0702@gmail.com
 * @since v0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuartzScheduleService {

    /** Quartz 스케줄러의 메인 인스턴스 */
    private final Scheduler scheduler;

    /**
     * DB에 정의된 Schedule 객체를 기반으로 Quartz에 스케줄을 등록하거나 업데이트합니다.
     *
     * <p>지정된 워크플로우 ID에 해당하는 Job이 이미 존재할 경우, 기존 Job과 Trigger를 삭제하고 새로운 정보로 다시 생성하여 스케줄을
     * 업데이트합니다. {@code JobDataMap}을 통해 실행될 Job에게 어떤 워크플로우를 실행해야 하는지 ID를 전달합니다.
     *
     * @param schedule Quartz에 등록할 스케줄 정보를 담은 도메인 모델 객체
     * @since v0.1.0
     */
    public void addOrUpdateSchedule(Schedule schedule) {
        try {
            JobKey jobKey = JobKey.jobKey("workflow-" + schedule.getWorkflowId());
            JobDetail jobDetail =
                    JobBuilder.newJob(WorkflowTriggerJob.class)
                            .withIdentity(jobKey)
                            .withDescription("Workflow " + schedule.getWorkflowId() + " Trigger Job")
                            .usingJobData("workflowId", schedule.getWorkflowId())
                            .storeDurably()
                            .build();

            TriggerKey triggerKey = TriggerKey.triggerKey("trigger-for-workflow-" + schedule.getWorkflowId());
            Trigger trigger =
                    TriggerBuilder.newTrigger()
                            .forJob(jobDetail)
                            .withIdentity(triggerKey)
                            .withSchedule(CronScheduleBuilder.cronSchedule(schedule.getCronExpression()))
                            .build();

            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey); // 기존 Job 삭제 후 재생성 (업데이트)
            }
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Quartz 스케줄 등록/업데이트 완료: Workflow ID {}", schedule.getWorkflowId());
        } catch (SchedulerException e) {
            log.error("Quartz 스케줄 등록 실패: Workflow ID " + schedule.getWorkflowId(), e);
            throw new RuntimeException("Quartz 스케줄 등록 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 지정된 워크플로우 ID와 연결된 Quartz 스케줄을 삭제합니다.
     *
     * @param workflowId 삭제할 스케줄에 연결된 워크플로우의 ID
     * @since v0.1.0
     */
    public void deleteSchedule(Long workflowId) {
        try {
            JobKey jobKey = JobKey.jobKey("workflow-" + workflowId);
            if (scheduler.checkExists(jobKey)) {
                boolean deleted = scheduler.deleteJob(jobKey);
                if (deleted) {
                    log.info("Quartz 스케줄 삭제 완료: Workflow ID {}", workflowId);
                } else {
                    log.warn("Quartz 스케줄 삭제 실패: Workflow ID {}", workflowId);
                }
            } else {
                log.debug("삭제할 Quartz 스케줄이 존재하지 않음: Workflow ID {}", workflowId);
            }
        } catch (SchedulerException e) {
            log.error("Quartz 스케줄 삭제 실패: Workflow ID " + workflowId, e);
            throw new RuntimeException("Quartz 스케줄 삭제 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 워크플로우와 연결된 모든 Quartz 스케줄을 일괄 삭제합니다.
     *
     * <p>하나의 워크플로우에 여러 스케줄이 있을 수 있으므로, 관련된 모든 Job을 제거합니다.
     *
     * @param workflowId 워크플로우 ID
     * @return 삭제된 스케줄 개수
     */
    public int deleteAllSchedulesForWorkflow(Long workflowId) {
        try {
            int deletedCount = 0;

            // 워크플로우 관련 모든 Job 키 조회
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());

            for (JobKey jobKey : jobKeys) {
                // "workflow-{workflowId}" 형식의 Job 찾기
                if (jobKey.getName().equals("workflow-" + workflowId)) {
                    boolean deleted = scheduler.deleteJob(jobKey);
                    if (deleted) {
                        deletedCount++;
                        log.debug("Quartz Job 삭제: {}", jobKey);
                    }
                }
            }

            log.info("Quartz 스케줄 일괄 삭제 완료: Workflow ID {} - {}개 삭제", workflowId, deletedCount);
            return deletedCount;

        } catch (SchedulerException e) {
            log.error("Quartz 스케줄 일괄 삭제 실패: Workflow ID " + workflowId, e);
            throw new RuntimeException("Quartz 스케줄 일괄 삭제 중 오류가 발생했습니다", e);
        }
    }

    /**
     * Quartz 스케줄러에 등록된 모든 Job 목록을 조회합니다.
     *
     * <p>디버깅 및 모니터링 용도로 사용됩니다.
     *
     * @return 등록된 Job 키 목록
     */
    public Set<JobKey> getAllScheduledJobs() {
        try {
            return scheduler.getJobKeys(GroupMatcher.anyJobGroup());
        } catch (SchedulerException e) {
            log.error("Quartz Job 목록 조회 실패", e);
            throw new RuntimeException("Quartz Job 목록 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 특정 워크플로우의 Quartz 스케줄이 등록되어 있는지 확인합니다.
     *
     * @param workflowId 워크플로우 ID
     * @return 등록되어 있으면 true
     */
    public boolean isScheduleRegistered(Long workflowId) {
        try {
            JobKey jobKey = JobKey.jobKey("workflow-" + workflowId);
            return scheduler.checkExists(jobKey);
        } catch (SchedulerException e) {
            log.error("Quartz 스케줄 존재 확인 실패: Workflow ID " + workflowId, e);
            return false;
        }
    }
}