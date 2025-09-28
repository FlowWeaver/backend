package site.icebang.domain.workflow.dto;

import lombok.Data;

@Data
public class RequestContext {

    private final String traceId;
    private final String clientIp;
    private final String userAgent;

    public static RequestContext forScheduler(String traceId) {
        return new RequestContext(
                traceId,
                "scheduler",
                "quartz-scheduler"
        );
    }
}
