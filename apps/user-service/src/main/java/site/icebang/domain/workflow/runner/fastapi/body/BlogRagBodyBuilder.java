package site.icebang.domain.workflow.runner.fastapi.body;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

import site.icebang.domain.workflow.model.JobRun;
import site.icebang.domain.workflow.model.Task;
import site.icebang.domain.workflow.service.WorkflowContextService;

@Component
@RequiredArgsConstructor
public class BlogRagBodyBuilder implements TaskBodyBuilder {

  private final ObjectMapper objectMapper;
  private final WorkflowContextService contextService; // 📌 컨텍스트 서비스 주입
  private static final String TASK_NAME = "블로그 RAG 생성 태스크";

  // 📌 데이터 소스가 되는 이전 Task들의 이름
  private static final String KEYWORD_SOURCE_TASK = "키워드 검색 태스크";
  private static final String PRODUCT_SELECT_SOURCE_TASK = "상품 선택 태스크";
  private static final String OCR_SOURCE_TASK = "이미지 OCR 태스크";

  @Override
  public boolean supports(String taskName) {
    return TASK_NAME.equals(taskName);
  }

  /**
   * 여러 이전 Task들의 결과를 DB에서 조회하고 조합하여 '블로그 RAG 생성'을 위한 Request Body를 생성합니다.
   *
   * @param task 실행할 Task의 도메인 모델
   * @param jobRun 현재 실행 중인 Job의 기록 객체 (이전 Task 결과를 조회하는 키로 사용)
   * @return 생성된 JSON Body
   */
  @Override
  public ObjectNode build(Task task, JobRun jobRun) {
    ObjectNode body = objectMapper.createObjectNode();

    // 1. '키워드 검색 태스크' 결과에서 키워드 정보 가져오기
    Optional<JsonNode> keywordResult =
        contextService.getPreviousTaskOutput(jobRun, KEYWORD_SOURCE_TASK);
    keywordResult
        .map(node -> node.path("data").path("keyword"))
        .ifPresent(keywordNode -> body.set("keyword", keywordNode));

    // 2. '이미지 OCR 태스크' 결과에서 번역 언어 정보 가져오기
    Optional<JsonNode> ocrResult = contextService.getPreviousTaskOutput(jobRun, OCR_SOURCE_TASK);
    ocrResult
        .map(node -> node.path("data").path("translation_language"))
        .filter(node -> !node.isMissingNode() && !node.asText().trim().isEmpty())
        .ifPresent(translationNode -> body.set("translation_language", translationNode));

    // 3. '상품 선택 태스크' 결과에서 선택된 상품 정보 가져오기
    Optional<JsonNode> productSelectResult =
        contextService.getPreviousTaskOutput(jobRun, PRODUCT_SELECT_SOURCE_TASK);
    productSelectResult
        .map(node -> node.path("data").path("selected_product"))
        .ifPresent(productNode -> body.set("product_info", productNode));

    return body;
  }
}
