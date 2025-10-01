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
public class WorkflowRunLogsResponseDto {
  private String traceId;
  private List<ExecutionLogDto> logs;
}
