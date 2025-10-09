package site.icebang.domain.workflow.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.Data;

import site.icebang.domain.schedule.dto.ScheduleDto;

@Data
public class WorkflowDetailCardDto extends WorkflowCardDto {
  private String defaultConfig;
  private Instant updatedAt;
  private String updatedBy;
  private List<ScheduleDto> schedules;
  private List<Map<String, Object>> jobs;
}
