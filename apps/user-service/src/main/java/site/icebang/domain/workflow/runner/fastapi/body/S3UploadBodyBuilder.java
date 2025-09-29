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
public class S3UploadBodyBuilder implements TaskBodyBuilder {

  private final ObjectMapper objectMapper;
  private final WorkflowContextService contextService; // 📌 컨텍스트 서비스 주입
  private static final String TASK_NAME = "S3 업로드 태스크";

  // 📌 데이터 소스가 되는 이전 Task들의 이름
  private static final String KEYWORD_SOURCE_TASK = "키워드 검색 태스크";
  private static final String CRAWL_SOURCE_TASK = "상품 정보 크롤링 태스크";

  @Override
  public boolean supports(String taskName) {
    return TASK_NAME.equals(taskName);
  }

  /**
   * 여러 이전 Task들의 결과를 DB에서 조회하고 조합하여
   * 'S3 업로드'를 위한 Request Body를 생성합니다.
   *
   * @param task      실행할 Task의 도메인 모델
   * @param jobRun    현재 실행 중인 Job의 기록 객체 (이전 Task 결과를 조회하는 키로 사용)
   * @return 생성된 JSON Body
   */
  @Override
  public ObjectNode build(Task task, JobRun jobRun) {
    ObjectNode body = objectMapper.createObjectNode();

    // 1. 컨텍스트 서비스를 통해 DB에서 '키워드 검색 태스크'의 결과를 조회
    Optional<JsonNode> keywordResult = contextService.getPreviousTaskOutput(jobRun, KEYWORD_SOURCE_TASK);
    keywordResult
            .map(node -> node.path("data").path("keyword"))
            .filter(node -> !node.isMissingNode() && !node.asText().trim().isEmpty())
            .ifPresent(keywordNode -> body.set("keyword", keywordNode));

    // 2. 컨텍스트 서비스를 통해 DB에서 '상품 정보 크롤링 태스크'의 결과를 조회
    Optional<JsonNode> crawlResult = contextService.getPreviousTaskOutput(jobRun, CRAWL_SOURCE_TASK);
    crawlResult
            .map(node -> node.path("data").path("crawled_products"))
            .filter(node -> !node.isMissingNode())
            .ifPresent(crawledProductsNode -> body.set("crawled_products", crawledProductsNode));

    // 3. 정적 데이터 설정
    body.put("base_folder", "product");

    return body;
  }
}