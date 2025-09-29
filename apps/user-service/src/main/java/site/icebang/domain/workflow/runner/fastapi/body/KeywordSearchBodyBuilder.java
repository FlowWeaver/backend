package site.icebang.domain.workflow.runner.fastapi.body;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

import site.icebang.domain.workflow.model.JobRun;
import site.icebang.domain.workflow.model.Task;

@Component
@RequiredArgsConstructor
public class KeywordSearchBodyBuilder implements TaskBodyBuilder {

  private final ObjectMapper objectMapper;
  private static final String TASK_NAME = "키워드 검색 태스크";

  @Override
  public boolean supports(String taskName) {
    return TASK_NAME.equals(taskName);
  }

  /**
   * Task에 주입된 사용자 정의 설정(settings)을 기반으로 Request Body를 생성합니다.
   *
   * @param task 실행할 Task의 도메인 모델 (settings 포함)
   * @param jobRun 현재 실행 중인 Job의 기록 객체 (이 빌더에서는 사용되지 않음)
   * @return 생성된 JSON Body (예: {"tag": "google"})
   */
  @Override
  public ObjectNode build(Task task, JobRun jobRun) {
    // 📌 Task에 동적으로 주입된 settings에서 'tag' 값을 가져옵니다.
    //    settings가 없거나 'tag' 필드가 없으면 기본값으로 "naver"를 사용합니다.
    String tag =
        Optional.ofNullable(task.getSettings())
            .map(settings -> settings.path("tag").asText("naver"))
            .orElse("naver");

    return objectMapper.createObjectNode().put("tag", tag);
  }
}
