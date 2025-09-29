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
public class BlogPublishBodyBuilder implements TaskBodyBuilder {

    private final ObjectMapper objectMapper;
    private final WorkflowContextService contextService;
    private static final String TASK_NAME = "블로그 발행 태스크";
    private static final String RAG_SOURCE_TASK = "블로그 RAG 생성 태스크";

    @Override
    public boolean supports(String taskName) {
        return TASK_NAME.equals(taskName);
    }

    @Override
    public ObjectNode build(Task task, JobRun jobRun) {
        ObjectNode body = objectMapper.createObjectNode();

        Optional<JsonNode> ragResultOpt = contextService.getPreviousTaskOutput(jobRun, RAG_SOURCE_TASK);
        ragResultOpt.ifPresent(ragResult -> {
            JsonNode data = ragResult.path("data");

            // 📌 1. .path()로 노드를 가져옵니다.
            JsonNode titleNode = data.path("title");
            // 📌 2. .isMissingNode()로 노드가 존재하는지 확인합니다.
            if (!titleNode.isMissingNode()) {
                body.set("post_title", titleNode);
            }

            JsonNode contentNode = data.path("content");
            if (!contentNode.isMissingNode()) {
                body.set("post_content", contentNode);
            }

            JsonNode tagsNode = data.path("tags");
            if (!tagsNode.isMissingNode()) {
                body.set("post_tags", tagsNode);
            }
        });

        Optional<JsonNode> settingsOpt = Optional.ofNullable(task.getSettings());
        settingsOpt.ifPresent(settings -> {
            body.put("tag", settings.path("tag").asText());
            body.put("blog_name", settings.path("blog_name").asText());
            body.put("blog_id", settings.path("blog_id").asText());
            body.put("blog_pw", settings.path("blog_pw").asText());
        });

        return body;
    }
}