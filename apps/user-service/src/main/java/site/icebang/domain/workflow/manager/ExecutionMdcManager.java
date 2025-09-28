package site.icebang.domain.workflow.manager;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ExecutionMdcManager {
  private static final String SOURCE_ID = "sourceId";
  private static final String EXECUTION_TYPE = "executionType";
  private static final String TRACE_ID = "traceID";
  private static final String CLIENT_IP = "clientIp";
  private static final String USER_AGENT = "userAgent";

  public void setWorkflowContext(Long workflowId, String traceId, String clientIp, String userAgent) {
    MDC.put(SOURCE_ID, workflowId.toString());
    MDC.put(EXECUTION_TYPE, "WORKFLOW");
    MDC.put(TRACE_ID, traceId);

    if (clientIp != null) {
      MDC.put(CLIENT_IP, clientIp);
    }
    if (userAgent != null) {
      MDC.put(USER_AGENT, userAgent);
    }
  }

  public void setWorkflowContext(Long workflowId) {
    MDC.put(SOURCE_ID, workflowId.toString());
    MDC.put(EXECUTION_TYPE, "WORKFLOW");
  }

  public void setJobContext(Long jobRunId) {
    MDC.put(SOURCE_ID, jobRunId.toString());
    MDC.put(EXECUTION_TYPE, "JOB");
  }

  public void setTaskContext(Long taskRunId) {
    MDC.put(SOURCE_ID, taskRunId.toString());
    MDC.put(EXECUTION_TYPE, "TASK");
  }

  public void clearExecutionContext() {
    MDC.remove(SOURCE_ID);
    MDC.remove(EXECUTION_TYPE);
  }
}
