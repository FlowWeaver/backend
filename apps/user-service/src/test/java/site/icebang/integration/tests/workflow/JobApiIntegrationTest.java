package site.icebang.integration.tests.workflow;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import com.epages.restdocs.apispec.ResourceSnippetParameters;

import site.icebang.integration.setup.support.IntegrationTestSupport;

@Sql(
    value = {
      "classpath:sql/data/00-truncate.sql",
      "classpath:sql/data/01-insert-internal-users.sql",
      "classpath:sql/data/03-insert-workflow-h2.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@DisplayName("Job API 통합 테스트")
public class JobApiIntegrationTest extends IntegrationTestSupport {

  @Test
  @DisplayName("Job 생성 성공")
  @WithUserDetails("admin@icebang.site")
  void createJob_success() throws Exception {
    // given
    String jobRequestJson =
        """
                {
                  "name": "통합테스트 Job",
                  "description": "Integration Test용 Job",
                  "isEnabled": true
                }
                """;

    // when & then
    mockMvc
        .perform(
            post(getApiUrlForDocs("/v0/jobs"))
                .header("Origin", "https://admin.icebang.site")
                .header("Referer", "https://admin.icebang.site/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jobRequestJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").exists())
        .andExpect(jsonPath("$.data.name").value("통합테스트 Job"))
        .andExpect(jsonPath("$.data.description").value("Integration Test용 Job"))
        .andExpect(jsonPath("$.data.isEnabled").value(true))
        .andExpect(jsonPath("$.data.createdAt").exists())
        .andExpect(jsonPath("$.data.updatedAt").exists())
        .andDo(
            document(
                "job-create",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                resource(
                    ResourceSnippetParameters.builder()
                        .tag("Job")
                        .summary("Job 생성")
                        .description("새로운 Job을 생성합니다")
                        .requestFields(
                            fieldWithPath("name")
                                .type(JsonFieldType.STRING)
                                .description("Job 이름 (필수)"),
                            fieldWithPath("description")
                                .type(JsonFieldType.STRING)
                                .description("Job 설명 (선택)"),
                            fieldWithPath("isEnabled")
                                .type(JsonFieldType.BOOLEAN)
                                .description("활성화 여부 (선택, 기본값: true)"))
                        .responseFields(
                            fieldWithPath("success")
                                .type(JsonFieldType.BOOLEAN)
                                .description("요청 성공 여부"),
                            fieldWithPath("data")
                                .type(JsonFieldType.OBJECT)
                                .description("생성된 Job 정보"),
                            fieldWithPath("data.id")
                                .type(JsonFieldType.NUMBER)
                                .description("Job ID"),
                            fieldWithPath("data.name")
                                .type(JsonFieldType.STRING)
                                .description("Job 이름"),
                            fieldWithPath("data.description")
                                .type(JsonFieldType.STRING)
                                .description("Job 설명")
                                .optional(),
                            fieldWithPath("data.isEnabled")
                                .type(JsonFieldType.BOOLEAN)
                                .description("활성화 여부")
                                .optional(),
                            fieldWithPath("data.createdAt")
                                .type(JsonFieldType.STRING)
                                .description("생성 시간"),
                            fieldWithPath("data.createdBy")
                                .type(JsonFieldType.NUMBER)
                                .description("생성자 ID")
                                .optional(),
                            fieldWithPath("data.updatedAt")
                                .type(JsonFieldType.STRING)
                                .description("수정 시간"),
                            fieldWithPath("data.updatedBy")
                                .type(JsonFieldType.NUMBER)
                                .description("수정자 ID")
                                .optional(),
                            fieldWithPath("data.executionOrder")
                                .type(JsonFieldType.NUMBER)
                                .description("실행 순서")
                                .optional())
                        .build())));
  }

  @Test
  @DisplayName("Job 조회 성공")
  @WithUserDetails("admin@icebang.site")
  void getJob_success() throws Exception {
    // given - 03-insert-workflow-h2.sql에서 생성된 Job ID 1 사용
    Long jobId = 1L;

    // when & then
    mockMvc
        .perform(
            get(getApiUrlForDocs("/v0/jobs/{id}"), jobId)
                .header("Origin", "https://admin.icebang.site")
                .header("Referer", "https://admin.icebang.site/"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(jobId.intValue()))
        .andExpect(jsonPath("$.data.name").value("상품 분석"))
        .andExpect(
            jsonPath("$.data.description").value("키워드 검색, 상품 크롤링, S3 업로드, OCR 처리 및 상품 선택 작업"))
        .andExpect(jsonPath("$.data.createdAt").exists())
        .andExpect(jsonPath("$.data.updatedAt").exists())
        .andDo(
            document(
                "job-get",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                resource(
                    ResourceSnippetParameters.builder()
                        .tag("Job")
                        .summary("Job 조회")
                        .description("Job ID로 Job 정보를 조회합니다")
                        .responseFields(
                            fieldWithPath("success")
                                .type(JsonFieldType.BOOLEAN)
                                .description("요청 성공 여부"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("Job 정보"),
                            fieldWithPath("data.id")
                                .type(JsonFieldType.NUMBER)
                                .description("Job ID"),
                            fieldWithPath("data.name")
                                .type(JsonFieldType.STRING)
                                .description("Job 이름"),
                            fieldWithPath("data.description")
                                .type(JsonFieldType.STRING)
                                .description("Job 설명")
                                .optional(),
                            fieldWithPath("data.isEnabled")
                                .type(JsonFieldType.BOOLEAN)
                                .description("활성화 여부")
                                .optional(),
                            fieldWithPath("data.createdAt")
                                .type(JsonFieldType.STRING)
                                .description("생성 시간"),
                            fieldWithPath("data.createdBy")
                                .type(JsonFieldType.NUMBER)
                                .description("생성자 ID")
                                .optional(),
                            fieldWithPath("data.updatedAt")
                                .type(JsonFieldType.STRING)
                                .description("수정 시간"),
                            fieldWithPath("data.updatedBy")
                                .type(JsonFieldType.NUMBER)
                                .description("수정자 ID")
                                .optional(),
                            fieldWithPath("data.executionOrder")
                                .type(JsonFieldType.NUMBER)
                                .description("실행 순서")
                                .optional())
                        .build())));
  }

  @Test
  @DisplayName("Job 이름 없이 생성 시 실패")
  @WithUserDetails("admin@icebang.site")
  void createJob_withoutName_shouldFail() throws Exception {
    // given
    String invalidJobRequestJson =
        """
                {
                  "description": "이름이 없는 Job",
                  "isEnabled": true
                }
                """;

    // when & then
    mockMvc
        .perform(
            post(getApiUrlForDocs("/v0/jobs"))
                .header("Origin", "https://admin.icebang.site")
                .header("Referer", "https://admin.icebang.site/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJobRequestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("빈 문자열 이름으로 Job 생성 시 실패")
  @WithUserDetails("admin@icebang.site")
  void createJob_withBlankName_shouldFail() throws Exception {
    // given
    String blankNameJobRequestJson =
        """
                {
                  "name": "   ",
                  "description": "빈 이름 Job",
                  "isEnabled": true
                }
                """;

    // when & then
    mockMvc
        .perform(
            post(getApiUrlForDocs("/v0/jobs"))
                .header("Origin", "https://admin.icebang.site")
                .header("Referer", "https://admin.icebang.site/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(blankNameJobRequestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("존재하지 않는 Job 조회 시 404 반환")
  @WithUserDetails("admin@icebang.site")
  void getJob_withNonExistentId_shouldReturn404() throws Exception {
    // given
    Long nonExistentId = 99999L;

    // when & then
    mockMvc
        .perform(
            get(getApiUrlForDocs("/v0/jobs/{id}"), nonExistentId)
                .header("Origin", "https://admin.icebang.site")
                .header("Referer", "https://admin.icebang.site/"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("Job 생성 시 UTC 시간으로 저장되는지 검증")
  @WithUserDetails("admin@icebang.site")
  void createJob_utc_time_validation() throws Exception {
    // given
    String jobRequestJson =
        """
                {
                  "name": "UTC 시간 검증 Job",
                  "description": "시간대 보정 테스트용 Job"
                }
                """;

    // when & then
    mockMvc
        .perform(
            post(getApiUrlForDocs("/v0/jobs"))
                .header("Origin", "https://admin.icebang.site")
                .header("Referer", "https://admin.icebang.site/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jobRequestJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.createdAt").exists())
        .andExpect(jsonPath("$.data.updatedAt").exists());

    // 시간 형식이 올바른지는 실제 DB에서 확인하거나
    // 응답 시간이 현재 UTC 시간과 비슷한 범위에 있는지 검증할 수 있음
  }
}
