package site.icebang.integration.tests.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import site.icebang.domain.workflow.dto.TaskDto;
import site.icebang.domain.workflow.service.WorkflowService;
import site.icebang.integration.setup.support.IntegrationTestSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TaskApiIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private WorkflowService workflowService;

  @Test
  @DisplayName("Task 생성 API - 성공")
  @WithMockUser(roles = "SUPER_ADMIN") // 📌 DB 조회 없이 'SUPER_ADMIN' 권한을 가진 가상 유저로 인증
  void createTask_success() throws Exception {
    // given
    TaskDto requestDto = new TaskDto();
    requestDto.setName("테스트 태스크");
    requestDto.setType("FastAPI");

    TaskDto createdDto = new TaskDto();
    createdDto.setId(1L);
    createdDto.setName("테스트 태스크");
    createdDto.setType("FastAPI");

    when(workflowService.createTask(any(TaskDto.class))).thenReturn(createdDto);

    // when & then
    mockMvc.perform(post("/v0/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1L))
            .andExpect(jsonPath("$.data.name").value("테스트 태스크"));
  }

  @Test
  @DisplayName("Task 조회 API - 성공")
  @WithMockUser(roles = "SUPER_ADMIN") // 📌 가상 유저로 인증
  void getTask_success() throws Exception {
    // given
    Long taskId = 1L;
    TaskDto foundDto = new TaskDto();
    foundDto.setId(taskId);
    foundDto.setName("조회된 태스크");
    foundDto.setType("FastAPI");

    when(workflowService.findTaskById(taskId)).thenReturn(foundDto);

    // when & then
    mockMvc.perform(get("/v0/tasks/{id}", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(taskId))
            .andExpect(jsonPath("$.data.name").value("조회된 태스크"));
  }

  @Test
  @DisplayName("Task 조회 API - 실패 (존재하지 않는 ID)")
  @WithMockUser(roles = "SUPER_ADMIN") // 📌 가상 유저로 인증
  void getTask_notFound() throws Exception {
    // given
    Long nonExistentTaskId = 999L;
    when(workflowService.findTaskById(nonExistentTaskId)).thenReturn(null);

    // when & then
    mockMvc.perform(get("/v0/tasks/{id}", nonExistentTaskId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
  }
}