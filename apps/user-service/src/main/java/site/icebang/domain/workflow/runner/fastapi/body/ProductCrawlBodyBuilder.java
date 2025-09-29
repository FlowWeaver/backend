package site.icebang.domain.workflow.runner.fastapi.body;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

import site.icebang.domain.workflow.model.JobRun;
import site.icebang.domain.workflow.model.Task;
import site.icebang.domain.workflow.service.WorkflowContextService;

@Component
@RequiredArgsConstructor
public class ProductCrawlBodyBuilder implements TaskBodyBuilder {

  private final ObjectMapper objectMapper;
  private final WorkflowContextService contextService; // 📌 컨텍스트 서비스 주입
  private static final String TASK_NAME = "상품 정보 크롤링 태스크";
  private static final String SIMILARITY_SOURCE_TASK = "상품 유사도 분석 태스크";

  @Override
  public boolean supports(String taskName) {
    return TASK_NAME.equals(taskName);
  }

  /**
   * 이전 Task 결과(유사도 분석 결과)를 DB에서 조회하여 크롤링할 상품 URL 목록으로 구성된 Request Body를 생성합니다.
   *
   * @param task 실행할 Task의 도메인 모델
   * @param jobRun 현재 실행 중인 Job의 기록 객체
   * @return 생성된 JSON Body (예: {"product_urls": ["url1", "url2", ...]})
   */
  @Override
  public ObjectNode build(Task task, JobRun jobRun) {
    ObjectNode body = objectMapper.createObjectNode();
    ArrayNode productUrls = objectMapper.createArrayNode();

    // 📌 컨텍스트 서비스를 통해 DB에서 '상품 유사도 분석 태스크'의 결과를 조회합니다.
    Optional<JsonNode> sourceResult =
        contextService.getPreviousTaskOutput(jobRun, SIMILARITY_SOURCE_TASK);

    sourceResult.ifPresent(
        node -> {
          JsonNode topProducts = node.path("data").path("top_products");
          if (topProducts.isArray()) {
            topProducts.forEach(
                product -> {
                  JsonNode urlNode = product.path("url");
                  if (!urlNode.isMissingNode()
                      && urlNode.isTextual()
                      && !urlNode.asText().isEmpty()) {
                    productUrls.add(urlNode.asText());
                  }
                });
          }
        });

    body.set("product_urls", productUrls);
    return body;
  }
}
