package site.icebang.domain.workflow.dto.log;

import java.math.BigInteger;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowLogQueryCriteriaDto {
  private final String traceId;
  private final BigInteger sourceId;

  @Pattern(regexp = "^(WORKFLOW|JOB|TASK)$", message = "실행 타입은 WORKFLOW, JOB, TASK 중 하나여야 합니다")
  private final String executionType;
}
