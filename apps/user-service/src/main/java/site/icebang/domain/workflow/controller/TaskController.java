package site.icebang.domain.workflow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import site.icebang.common.dto.ApiResponse;
import site.icebang.domain.workflow.dto.TaskDto;
import site.icebang.domain.workflow.model.TaskIoData;
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

  /**
   * Task Run ID 목록으로 Task IO 데이터 조회
   *
   * @param taskRunIds Task Run ID 목록 (쉼표로 구분)
   * @param ioType IO 타입 필터 ("INPUT", "OUTPUT", 미지정시 모두 조회)
   * @param limit 조회 제한 수 (선택사항)
   * @return Task IO 데이터 목록 (created_at 기준 내림차순 정렬)
   */
  @GetMapping("/io-data")
  public ResponseEntity<ApiResponse<List<TaskIoData>>> getTaskIoData(
      @RequestParam List<Long> taskRunIds,
      @RequestParam(required = false) String ioType,
      @RequestParam(required = false) Integer limit) {

    try {
      List<TaskIoData> ioData =
          workflowService.getTaskIoDataByTaskRunIds(taskRunIds, ioType, limit);
      return ResponseEntity.ok(ApiResponse.success(ioData, "Task IO 데이터 조회 성공"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              ApiResponse.error(
                  "Task IO 데이터 조회 실패: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }
  }
}
