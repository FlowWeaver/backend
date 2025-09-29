package site.icebang.integration.tests.workflow;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import com.epages.restdocs.apispec.ResourceSnippetParameters;

import site.icebang.domain.workflow.service.WorkflowExecutionService;
import site.icebang.integration.setup.support.IntegrationTestSupport;

@Sql(
    value = {
      "classpath:sql/data/01-insert-internal-users.sql",
      "classpath:sql/data/03-insert-workflow-h2.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class WorkflowRunApiIntegrationTest extends IntegrationTestSupport {

  @MockitoBean private WorkflowExecutionService mockWorkflowExecutionService;

  @Test
  @DisplayName("워크플로우 수동 실행 API 호출 성공")
  @WithUserDetails("admin@icebang.site")
  void runWorkflow_success() throws Exception {
    // given
    Long workflowId = 1L;

    // when & then
    mockMvc
        .perform(
            post(getApiUrlForDocs("/v0/workflows/{workflowId}/run"), workflowId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Origin", "https://admin.icebang.site")
                .header("Referer", "https://admin.icebang.site/"))
        .andExpect(status().isAccepted()) // 📌 1. 즉시 202 Accepted 응답을 받는지 확인
        .andDo(
            document(
                "workflow-run",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                resource(
                    ResourceSnippetParameters.builder()
                        .tag("Workflow Execution")
                        .summary("워크플로우 수동 실행")
                        .description(
                            "지정된 ID의 워크플로우를 즉시 비동기적으로 실행합니다. "
                                + "성공 시 202 Accepted를 반환하며, 실제 실행은 백그라운드에서 진행됩니다.")
                        .build())));

    // 📌 2. 비동기 호출된 executeWorkflow 메소드가 1초 이내에 1번 실행되었는지 검증
    verify(mockWorkflowExecutionService, timeout(1000).times(1)).executeWorkflow(eq(workflowId));
  }
}
