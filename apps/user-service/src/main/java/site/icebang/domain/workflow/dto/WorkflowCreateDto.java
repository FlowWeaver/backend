package site.icebang.domain.workflow.dto;

import java.math.BigInteger;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import site.icebang.domain.schedule.dto.ScheduleCreateDto;

/**
 * 워크플로우 생성 요청 DTO
 *
 * <p>프론트엔드에서 워크플로우 생성 시 필요한 모든 정보를 담는 DTO - 기본 정보: 이름, 설명 - 플랫폼 설정: 검색 플랫폼, 포스팅 플랫폼 - 계정 설정: 포스팅 계정
 * 정보 (JSON 형태로 저장) - 스케줄 설정: 선택적으로 여러 스케줄 등록 가능
 *
 * @author bwnfo0702@gmail.com
 * @since v0.1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowCreateDto {
  @Null private BigInteger id;

  @NotBlank(message = "워크플로우 이름은 필수입니다")
  @Size(max = 100, message = "워크플로우 이름은 100자를 초과할 수 없습니다")
  @Pattern(
      regexp = "^[가-힣a-zA-Z0-9\\s_-]+$",
      message = "워크플로우 이름은 한글, 영문, 숫자, 공백, 언더스코어, 하이픈만 사용 가능합니다")
  private String name;

  @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
  private String description;

  @Pattern(regexp = "^(naver|naver_store)?$", message = "검색 플랫폼은 'naver' 또는 'naver_store'만 가능합니다")
  @JsonProperty("search_platform")
  private String searchPlatform;

  @Pattern(
      regexp = "^(naver_blog|tstory_blog|blogger)?$",
      message = "포스팅 플랫폼은 'naver_blog', 'tstory_blog', 'blogger' 중 하나여야 합니다")
  @JsonProperty("posting_platform")
  private String postingPlatform;

  @Size(max = 100, message = "포스팅 계정 ID는 100자를 초과할 수 없습니다")
  @JsonProperty("posting_account_id")
  private String postingAccountId;

  @Size(max = 200, message = "포스팅 계정 비밀번호는 200자를 초과할 수 없습니다")
  @JsonProperty("posting_account_password")
  private String postingAccountPassword;

  @Size(max = 100, message = "블로그 이름은 100자를 초과할 수 없습니다")
  @JsonProperty("blog_name")
  private String blogName;

  @Builder.Default
  @JsonProperty("is_enabled")
  private Boolean isEnabled = true;

  /**
   * 워크플로우에 등록할 스케줄 목록 (선택사항)
   *
   * <p>사용 시나리오:
   *
   * <ul>
   *   <li>null 또는 빈 리스트: 스케줄 없이 워크플로우만 생성
   *   <li>1개 이상: 해당 스케줄들을 함께 등록 (트랜잭션 보장)
   * </ul>
   */
  @Valid private List<@Valid ScheduleCreateDto> schedules;

  // JSON 변환용 필드 (MyBatis에서 사용)
  private String defaultConfigJson;

  public String generateDefaultConfigJson() {
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("{");

    // 크롤링 플랫폼 설정 (키: "1")
    if (searchPlatform != null && !searchPlatform.isBlank()) {
      jsonBuilder.append("\"1\": {\"tag\": \"").append(searchPlatform).append("\"}");
    }

    // 포스팅 설정 (키: "8")
    if (hasPostingConfig()) {
      if (jsonBuilder.length() > 1) {
        jsonBuilder.append(", ");
      }
      jsonBuilder
          .append("\"8\": {")
          .append("\"tag\": \"")
          .append(postingPlatform)
          .append("\", ")
          .append("\"blog_id\": \"")
          .append(postingAccountId)
          .append("\", ")
          .append("\"blog_pw\": \"")
          .append(postingAccountPassword)
          .append("\"");

      // tstory_blog인 경우 blog_name 추가
      if ("tstory_blog".equals(postingPlatform) && blogName != null && !blogName.isBlank()) {
        jsonBuilder.append(", \"blog_name\": \"").append(blogName).append("\"");
      }

      jsonBuilder.append("}");
    }

    jsonBuilder.append("}");
    return jsonBuilder.toString();
  }

  // 포스팅 설정 완성도 체크 (상태 확인 유틸)
  public boolean hasPostingConfig() {
    return postingPlatform != null
        && !postingPlatform.isBlank()
        && postingAccountId != null
        && !postingAccountId.isBlank()
        && postingAccountPassword != null
        && !postingAccountPassword.isBlank();
  }

  /**
   * 스케줄 설정이 있는지 확인
   *
   * @return 스케줄이 1개 이상 있으면 true
   */
  public boolean hasSchedules() {
    return schedules != null && !schedules.isEmpty();
  }
}
