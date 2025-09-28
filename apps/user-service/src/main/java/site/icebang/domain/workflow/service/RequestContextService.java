package site.icebang.domain.workflow.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import site.icebang.domain.workflow.dto.RequestContext;

import java.util.UUID;

@Service
public class RequestContextService {

    public RequestContext extractRequestContext() {
        String traceId = MDC.get("traceId") != null ? MDC.get("traceId") : UUID.randomUUID().toString();
        String clientIp = MDC.get("clientIp");
        String userAgent = MDC.get("userAgent");

        return new RequestContext(traceId, clientIp, userAgent);
    }

    public RequestContext quartzContext() {
        String traceId = MDC.get("traceId") != null ? MDC.get("traceId") : UUID.randomUUID().toString();

        return RequestContext.forScheduler(traceId);
    }
}
