package site.icebang.domain.workflow.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobDto {
  private Long id;

  @NotBlank(message = "Job 이름은 필수입니다")
  private String name;

  private String description;
  private Boolean isEnabled;
  private Instant createdAt;
  private Long createdBy;
  private Instant updatedAt;
  private Long updatedBy;

  private Integer executionOrder;
}
