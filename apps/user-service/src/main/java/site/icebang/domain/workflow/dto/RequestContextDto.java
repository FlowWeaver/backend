package site.icebang.domain.workflow.dto;

import lombok.Data;

/** 요청 컨텍스트 정보를 담는 DTO 클래스 분산 추적, 클라이언트 정보 등을 포함하여 워크플로우 실행 시 필요한 컨텍스트를 관리합니다. */
@Data
public class RequestContextDto {

  private final String traceId;
  private final String clientIp;
  private final String userAgent;

  /**
   * 스케줄러 실행용 컨텍스트를 생성하는 정적 팩토리 메서드 HTTP 요청이 아닌 스케줄된 작업에서 사용됩니다.
   *
   * @param traceId 분산 추적 ID
   * @return 스케줄러용 RequestContext 객체 (clientIp와 userAgent는 기본값 설정)
   */
  public static RequestContextDto forScheduler(String traceId) {
    return new RequestContextDto(traceId, "scheduler", "quartz-scheduler");
  }
}
