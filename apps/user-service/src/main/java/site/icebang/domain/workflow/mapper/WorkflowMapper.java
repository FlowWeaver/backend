package site.icebang.domain.workflow.mapper;

import java.math.BigInteger;
import java.util.*;

import org.apache.ibatis.annotations.Param;

import site.icebang.common.dto.PageParams;
import site.icebang.domain.schedule.dto.ScheduleDto;
import site.icebang.domain.workflow.dto.WorkflowCardDto;
import site.icebang.domain.workflow.dto.WorkflowDetailCardDto;

public interface WorkflowMapper {
  List<WorkflowCardDto> selectWorkflowList(PageParams pageParams);

  int selectWorkflowCount(PageParams pageParams);

  int insertWorkflow(Map<String, Object> params); // insert workflow

  // Job 생성 관련 메서드
  void insertJobs(Map<String, Object> params); // 여러 Job을 동적으로 생성

  void insertWorkflowJobs(Map<String, Object> params); // Workflow-Job 연결

  void insertJobTasks(Map<String, Object> params); // Job-Task 연결

  boolean existsByName(String name);

  WorkflowCardDto selectWorkflowById(BigInteger id);

  WorkflowDetailCardDto selectWorkflowDetailById(BigInteger workflowId);

  List<ScheduleDto> selectSchedulesByWorkflowId(BigInteger workflowId);

  List<Map<String, Object>> selectWorkflowWithJobsAndTasks(BigInteger workflowId);

  int updateWorkflowEnabled(
      @Param("workflowId") BigInteger workflowId, @Param("isEnabled") Boolean isEnabled);

  int markAsDeleted(@Param("workflowId") BigInteger workflowId);
}
