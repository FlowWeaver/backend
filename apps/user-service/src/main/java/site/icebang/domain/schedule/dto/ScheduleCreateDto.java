package site.icebang.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import site.icebang.domain.schedule.model.Schedule;

/**
 * 스케줄 생성 요청 DTO
 *
 * <p>워크플로우 생성 시 함께 등록할 스케줄 정보를 담습니다.
 *
 * <p>역할: - API 요청 시 클라이언트로부터 스케줄 정보 수신 - 입력값 검증 (형식, 길이 등) - Schedule(Model)로 변환되어 DB 저장
 *
 * <p>기존 ScheduleDto(응답용)와 통일성을 위해 camelCase 사용
 *
 * @author bwnfo0702@gmail.com
 * @since v0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCreateDto {
  /**
   * 크론 표현식 (필수)
   *
   * <p>Quartz 크론 표현식 형식을 따릅니다.
   *
   * <ul>
   *   <li>초 분 시 일 월 요일 (년도)
   *   <li>예시: "0 9 * * *" (매일 9시 0분 0초)
   *   <li>예시: "0 0 14 * * MON-FRI" (평일 오후 2시)
   * </ul>
   *
   * <p>정밀한 유효성 검증은 서비스 레이어에서 Quartz CronExpression으로 수행됩니다.
   */
  @NotBlank(message = "크론 표현식은 필수입니다")
  @Size(max = 50, message = "크론 표현식은 50자를 초과할 수 없습니다")
  private String cronExpression;

  /**
   * 사용자 친화적 스케줄 설명
   *
   * <p>UI에 표시될 스케줄 설명입니다. 자동으로 생성됩니다.
   *
   * <ul>
   *   <li>예시: "매일 오전 9시"
   *   <li>예시: "평일 오후 2시"
   *   <li>예시: "매주 금요일 6시"
   * </ul>
   */
  @Size(max = 20, message = "스케줄 설명은 자동으로 생성됩니다")
  private String scheduleText;

  /**
   * 스케줄 활성화 여부 (기본값: true)
   *
   * <p>false일 경우 DB에는 저장되지만 Quartz에 등록되지 않습니다.
   */
  @Builder.Default private Boolean isActive = true;

  /**
   * 스케줄 실행 시 추가 파라미터 (선택, JSON 형식)
   *
   * <p>워크플로우 실행 시 전달할 추가 파라미터를 JSON 문자열로 저장합니다.
   *
   * <pre>{@code
   * // 예시:
   * {
   *   "retryCount": 3,
   *   "timeout": 300,
   *   "notifyOnFailure": true
   * }
   * }</pre>
   */
  private String parameters;

  /**
   * Schedule(Model) 엔티티로 변환
   *
   * <p>DTO의 정보를 DB 저장용 엔티티로 변환하며, 서비스 레이어에서 주입되는 workflowId와 userId를 함께 설정합니다.
   *
   * @param dto 변환할 ScheduleCreateDto 객체
   * @param workflowId 연결할 워크플로우 ID
   * @param userId 생성자 ID
   * @return DB 저장 가능한 Schedule 엔티티
   */
  public static Schedule toEntity(ScheduleCreateDto dto, Long workflowId, Long userId) {
    return Schedule.builder()
            .workflowId(workflowId)
            .cronExpression(dto.cronExpression)
            .scheduleText(dto.scheduleText)
            .isActive(dto.isActive != null ? dto.isActive : true)
            .parameters(dto.parameters)
            .createdBy(userId)
            .updatedBy(userId)
            .build();
  }
}
