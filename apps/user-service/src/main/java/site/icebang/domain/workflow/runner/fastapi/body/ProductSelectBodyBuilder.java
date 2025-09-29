package site.icebang.domain.workflow.runner.fastapi.body;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.icebang.domain.workflow.model.JobRun;
import site.icebang.domain.workflow.model.Task;

@Component
@RequiredArgsConstructor
public class ProductSelectBodyBuilder implements TaskBodyBuilder {

  private final ObjectMapper objectMapper;
  private static final String TASK_NAME = "상품 선택 태스크";

  @Override
  public boolean supports(String taskName) {
    return TASK_NAME.equals(taskName);
  }

  /**
   * '상품 선택' Task를 위한 정적인 Request Body를 생성합니다.
   *
   * @param task      실행할 Task의 도메인 모델 (이 빌더에서는 사용되지 않음)
   * @param jobRun    현재 실행 중인 Job의 기록 객체 (이 빌더에서는 사용되지 않음)
   * @return 생성된 JSON Body (예: {"selection_criteria": "image_count_priority"})
   */
  @Override
  public ObjectNode build(Task task, JobRun jobRun) {
    ObjectNode body = objectMapper.createObjectNode();

    // 이 Task는 항상 고정된 선택 기준을 Body에 담아 보냅니다.
    body.put("selection_criteria", "image_count_priority");

    return body;
  }
}