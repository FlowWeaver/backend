package site.icebang.domain.workflow.service;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import site.icebang.domain.workflow.dto.RequestContext;

/** 요청 컨텍스트 정보를 추출하고 관리하는 서비스 MDC(Mapped Diagnostic Context)를 사용하여 분산 추적 정보를 처리합니다. */
@Service
public class RequestContextService {

  /**
   * HTTP 요청으로부터 컨텍스트 정보를 추출합니다.
   *
   * @return 추출된 요청 컨텍스트
   */
  public RequestContext extractRequestContext() {
    String traceId = MDC.get("traceId") != null ? MDC.get("traceId") : UUID.randomUUID().toString();
    String clientIp = MDC.get("clientIp");
    String userAgent = MDC.get("userAgent");

    return new RequestContext(traceId, clientIp, userAgent);
  }

  /**
   * Quartz 스케줄러용 컨텍스트를 생성합니다. 스케줄된 작업에서는 HTTP 요청 정보가 없으므로 traceId만 포함됩니다.
   *
   * @return 스케줄러용 요청 컨텍스트
   */
  public RequestContext quartzContext() {
    String traceId = MDC.get("traceId") != null ? MDC.get("traceId") : UUID.randomUUID().toString();

    return RequestContext.forScheduler(traceId);
  }
}
