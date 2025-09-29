package site.icebang.domain.workflow.runner.fastapi.body;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.icebang.domain.workflow.model.JobRun;
import site.icebang.domain.workflow.model.Task;
import site.icebang.domain.workflow.service.WorkflowContextService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ImageOcrBodyBuilder implements TaskBodyBuilder {

  private final ObjectMapper objectMapper;
  private final WorkflowContextService contextService; // 📌 컨텍스트 서비스 주입
  private static final String TASK_NAME = "이미지 OCR 태스크";
  private static final String SOURCE_TASK_NAME = "키워드 검색 태스크";

  @Override
  public boolean supports(String taskName) {
    return TASK_NAME.equals(taskName);
  }

  /**
   * 이전 Task 결과(키워드)를 DB에서 조회하여 OCR Task의 Request Body를 생성합니다.
   *
   * @param task      실행할 Task의 도메인 모델
   * @param jobRun    현재 실행 중인 Job의 기록 객체
   * @return 생성된 JSON Body
   */
  @Override
  public ObjectNode build(Task task, JobRun jobRun) {
    ObjectNode body = objectMapper.createObjectNode();

    // 📌 컨텍스트 서비스를 통해 DB에서 '키워드 검색 태스크'의 결과를 조회합니다.
    Optional<JsonNode> sourceResult = contextService.getPreviousTaskOutput(jobRun, SOURCE_TASK_NAME);

    sourceResult
            .map(result -> result.path("data").path("keyword"))
            .filter(node -> !node.isMissingNode() && !node.asText().trim().isEmpty())
            .ifPresent(keywordNode -> body.set("keyword", keywordNode));

    return body;
  }
}