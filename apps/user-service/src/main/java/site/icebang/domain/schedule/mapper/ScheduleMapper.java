package site.icebang.domain.schedule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import site.icebang.domain.schedule.model.Schedule;

/**
 * Schedule 데이터베이스 접근을 위한 MyBatis Mapper 인터페이스
 *
 * <p>워크플로우의 스케줄 정보를 관리하며, 한 워크플로우에 여러 스케줄을 등록할 수 있지만 같은 크론식의 중복 등록은 방지합니다.
 *
 * @author bwnfo0702@gmail.com
 * @since v0.1.0
 */
@Mapper
public interface ScheduleMapper {

  /**
   * 활성화된 모든 스케줄 조회
   *
   * @return 활성 상태인 스케줄 목록
   */
  List<Schedule> findAllActive();

  /**
   * 새로운 스케줄 등록
   *
   * @param schedule 등록할 스케줄 정보
   * @return 영향받은 행 수 (1: 성공, 0: 실패)
   */
  int insertSchedule(Schedule schedule);

  /**
   * 특정 워크플로우의 모든 활성 스케줄 조회
   *
   * @param workflowId 조회할 워크플로우 ID
   * @return 해당 워크플로우의 활성 스케줄 목록
   */
  List<Schedule> findAllByWorkflowId(@Param("workflowId") Long workflowId);

  /**
   * 특정 워크플로우에서 특정 크론식이 이미 존재하는지 확인
   *
   * @param workflowId 워크플로우 ID
   * @param cronExpression 확인할 크론 표현식
   * @return 중복 여부 (true: 이미 존재함, false: 존재하지 않음)
   */
  boolean existsByWorkflowIdAndCronExpression(
      @Param("workflowId") Long workflowId, @Param("cronExpression") String cronExpression);

  /**
   * 특정 워크플로우의 특정 크론식을 가진 스케줄 조회
   *
   * @param workflowId 워크플로우 ID
   * @param cronExpression 크론 표현식
   * @return 해당하는 스케줄 (없으면 null)
   */
  Schedule findByWorkflowIdAndCronExpression(
      @Param("workflowId") Long workflowId, @Param("cronExpression") String cronExpression);

  /**
   * 스케줄 활성화 상태 변경
   *
   * @param id 스케줄 ID
   * @param isActive 활성화 여부
   * @return 영향받은 행 수
   */
  int updateActiveStatus(@Param("id") Long id, @Param("isActive") boolean isActive);

  /**
   * 스케줄 정보 수정 (크론식, 설명 등)
   *
   * @param schedule 수정할 스케줄 정보
   * @return 영향받은 행 수
   */
  int updateSchedule(Schedule schedule);

  /**
   * 스케줄 삭제 (soft delete)
   *
   * @param id 삭제할 스케줄 ID
   * @return 영향받은 행 수
   */
  int deleteSchedule(@Param("id") Long id);

  /**
   * 워크플로우의 모든 스케줄 비활성화
   *
   * @param workflowId 워크플로우 ID
   * @return 영향받은 행 수
   */
  int deactivateAllByWorkflowId(@Param("workflowId") Long workflowId);

  /**
   * 스케줄 ID로 단건 조회
   *
   * @param id 스케줄 ID
   * @return 스케줄 정보, 없으면 null
   */
  Schedule findById(@Param("id") Long id);
}
