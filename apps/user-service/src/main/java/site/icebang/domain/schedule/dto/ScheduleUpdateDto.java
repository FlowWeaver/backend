package site.icebang.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 스케줄 수정 요청을 위한 DTO 클래스입니다.
 *
 * <p>기존 스케줄의 크론 표현식, 스케줄 텍스트, 활성화 상태 등을 수정할 때 사용합니다.
 *
 * <h2>검증 규칙:</h2>
 *
 * <ul>
 *   <li>cronExpression: 필수값, Quartz 크론식 형식
 *   <li>scheduleText: 선택값, 사용자 친화적 스케줄 설명 (예: "매일 오전 8시")
 *   <li>isActive: 필수값, 스케줄 활성화 여부
 * </ul>
 *
 * @author bwnfo0702@gmail.com
 * @since v0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleUpdateDto {

  /**
   * Quartz 크론 표현식
   *
   * <p>예시: "0 0 8 * * ?" (매일 오전 8시)
   */
  @NotBlank(message = "크론 표현식은 필수입니다")
  private String cronExpression;

  /**
   * 사용자 친화적 스케줄 설명 텍스트
   *
   * <p>예시: "매일 오전 8시", "매주 월요일 오후 6시"
   */
  private String scheduleText;

  /**
   * 스케줄 활성화 여부
   *
   * <p>true: 활성화 (실행됨), false: 비활성화 (실행 안 됨)
   */
  @NotNull(message = "활성화 상태는 필수입니다")
  private Boolean isActive;
}
