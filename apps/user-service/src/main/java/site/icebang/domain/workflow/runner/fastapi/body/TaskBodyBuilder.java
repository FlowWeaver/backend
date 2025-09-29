package site.icebang.domain.workflow.runner.fastapi.body;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import site.icebang.domain.workflow.model.JobRun;
import site.icebang.domain.workflow.model.Task;

public interface TaskBodyBuilder {

  /**
   * 이 빌더가 어떤 Task를 지원하는지 식별합니다.
   *
   * @param taskName Task의 고유한 이름
   * @return 지원하면 true, 아니면 false
   */
  boolean supports(String taskName);


  // 📌 workflowContext(Map) 대신 JobRun 객체를 받도록 변경
  ObjectNode build(Task task, JobRun jobRun);
}
