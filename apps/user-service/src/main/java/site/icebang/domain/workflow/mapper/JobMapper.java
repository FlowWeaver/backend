package site.icebang.domain.workflow.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import site.icebang.domain.workflow.dto.JobDto;
import site.icebang.domain.workflow.dto.TaskDto;

@Mapper
public interface JobMapper {
  List<JobDto> findJobsByWorkflowId(Long workflowId);

  List<TaskDto> findTasksByJobId(Long jobId);

  JobDto findJobById(Long id);

  void insertJob(JobDto job);
}
