package site.icebang.domain.workflow.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import site.icebang.domain.workflow.dto.JobDto;
import site.icebang.domain.workflow.service.WorkflowService;

@RestController
@RequestMapping("/v0/jobs")
@RequiredArgsConstructor
public class JobController {

  private final WorkflowService workflowService;

  @PostMapping
  public ResponseEntity<Map<String, Object>> createJob(@Valid @RequestBody JobDto dto) {
    JobDto created = workflowService.createJob(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", created));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Map<String, Object>> getJob(@PathVariable Long id) {
    JobDto job = workflowService.findJobById(id);
    if (job == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false));
    }
    return ResponseEntity.ok(Map.of("success", true, "data", job));
  }
}
