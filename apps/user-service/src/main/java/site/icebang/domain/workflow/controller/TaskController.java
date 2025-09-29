package site.icebang.domain.workflow.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import site.icebang.domain.workflow.dto.TaskDto;
import site.icebang.domain.workflow.service.WorkflowService;

@RestController
@RequestMapping("/v0/tasks")
@RequiredArgsConstructor
public class TaskController {

  private final WorkflowService workflowService;

  @PostMapping
  public ResponseEntity<Map<String, Object>> createTask(@Valid @RequestBody TaskDto dto) {
    TaskDto created = workflowService.createTask(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", created));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Map<String, Object>> getTask(@PathVariable Long id) {
    TaskDto task = workflowService.findTaskById(id);
    if (task == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false));
    }
    return ResponseEntity.ok(Map.of("success", true, "data", task));
  }
}
