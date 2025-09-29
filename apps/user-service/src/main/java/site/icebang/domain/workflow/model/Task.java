package site.icebang.domain.workflow.model;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import site.icebang.domain.workflow.dto.TaskDto;

@Getter
@NoArgsConstructor // MyBatis가 객체를 생성하기 위해 필요
@AllArgsConstructor
public class Task {

  private Long id;
  private String name;

  /** Task의 타입 (예: "HTTP", "SPRING_BATCH") 이 타입에 따라 TaskRunner가 선택됩니다. */
  private String type;

  /** Task 실행에 필요한 파라미터 (JSON) 예: {"url": "http://...", "method": "POST", "body": {...}} */
  private JsonNode parameters;

  private JsonNode settings;

  private Instant createdAt;

  private Instant updatedAt;

  public Task(TaskDto taskDto) {
    this.id = taskDto.getId();
    this.name = taskDto.getName();
    this.type = taskDto.getType();
    this.settings = taskDto.getSettings();
    this.parameters = taskDto.getParameters();
    this.createdAt = taskDto.getCreatedAt();
    this.updatedAt = taskDto.getUpdatedAt();
  }
}
