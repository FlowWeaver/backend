package site.icebang.domain.workflow.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunDetailResponseDto {
  private String traceId;
  private WorkflowRunDto workflowRun;
  private List<JobRunDto> jobRuns;
}
